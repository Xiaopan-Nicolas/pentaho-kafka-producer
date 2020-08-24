package com.ruckuswireless.pentaho.kafka.producer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map.Entry;
import java.util.Properties;

import javax.security.auth.login.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

/**
 * Kafka Producer step processor
 *
 * @author Michael Spector
 */
public class KafkaProducerStep extends BaseStep implements StepInterface {

	private final static byte[] getUTFBytes(String source) {
		if (source == null) {
			return null;
		}
		try {
			return source.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public KafkaProducerStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
			Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		KafkaProducerData data = (KafkaProducerData) sdi;
		if (data.producer != null) {
			data.producer.close();
			data.producer = null;
		}
		super.dispose(smi, sdi);
	}

	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		Object[] r = getRow();
		if (r == null) {
			setOutputDone();
			return false;
		}

		KafkaProducerMeta meta = (KafkaProducerMeta) smi;
		KafkaProducerData data = (KafkaProducerData) sdi;

		RowMetaInterface inputRowMeta = getInputRowMeta();

		if (first) {
			first = false;

			// Initialize Kafka client:
			if (data.producer == null) {
				Properties properties = meta.getKafkaProperties();
				Properties substProperties = new Properties();
				for (Entry<Object, Object> e : properties.entrySet()) {
					substProperties.put(e.getKey(), environmentSubstitute(e.getValue().toString()));
				}

				ProducerConfig producerConfig = new ProducerConfig(substProperties);
				logBasic(Messages.getString("KafkaProducerStep.CreateKafkaProducer.Message",
						producerConfig.brokerList()));
				// kerberos
				String isSecurity = substProperties.getProperty("isSecureMode");
				if("true".equalsIgnoreCase(isSecurity)){
					if(StringUtils.isBlank(substProperties.getProperty("security.protocol"))){
						substProperties.setProperty("security.protocol","SASL_PLAINTEXT");
					}
					if(StringUtils.isBlank(substProperties.getProperty("sasl.kerberos.service.name"))){
						substProperties.setProperty("sasl.kerberos.service.name","kafka");
					}
					if(StringUtils.isBlank(substProperties.getProperty("kerberos.domain.name"))){
						substProperties.setProperty("kerberos.domain.name","hadoop.hadoop.com");
					}
					if(StringUtils.isBlank(substProperties.getProperty("key.serializer"))){
						substProperties.setProperty("key.serializer", StringSerializer.class.getName());
					}
					if(StringUtils.isBlank(substProperties.getProperty("value.serializer"))){
						substProperties.setProperty("value.serializer", StringSerializer.class.getName());
					}
					System.setProperty("zookeeper.server.principal", substProperties.getProperty("zookeeper.server.principal"));
					String confPath = System.getProperty("user.dir") + File.separator + "conf" + File.separator;
					String krb5Conf = confPath + "krb5.conf";
					System.setProperty("java.security.krb5.conf", krb5Conf);
				}
				substProperties.setProperty("bootstrap.servers",producerConfig.brokerList());
				substProperties.setProperty("client.id",producerConfig.clientId());
				Thread.currentThread().setContextClassLoader(null);
				logBasic("此客户端配置的kerberos krb5.conf 配置：" + System.getProperty("java.security.krb5.conf"));
				logBasic("此客户端配置的kerberos jaas.conf 配置：" + System.getProperty("java.security.auth.login.config"));
				Configuration.setConfiguration(null);
				data.producer = new KafkaProducer<String, String>(substProperties);
			}

			data.outputRowMeta = getInputRowMeta().clone();
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this);

			int numErrors = 0;

			String messageField = environmentSubstitute(meta.getMessageField());

			if (KafkaProducerMeta.isEmpty(messageField)) {
				logError(Messages.getString("KafkaProducerStep.Log.MessageFieldNameIsNull")); //$NON-NLS-1$
				numErrors++;
			}
			data.messageFieldNr = inputRowMeta.indexOfValue(messageField);
			if (data.messageFieldNr < 0) {
				logError(Messages.getString("KafkaProducerStep.Log.CouldntFindField", messageField)); //$NON-NLS-1$
				numErrors++;
			}
			if (!inputRowMeta.getValueMeta(data.messageFieldNr).isBinary()
					&& !inputRowMeta.getValueMeta(data.messageFieldNr).isString()) {
				logError(Messages.getString("KafkaProducerStep.Log.FieldNotValid", messageField)); //$NON-NLS-1$
				numErrors++;
			}
			data.messageIsString = inputRowMeta.getValueMeta(data.messageFieldNr).isString();
			data.messageFieldMeta = inputRowMeta.getValueMeta(data.messageFieldNr);

			String keyField = environmentSubstitute(meta.getKeyField());

			if (!KafkaProducerMeta.isEmpty(keyField)) {
				logBasic(Messages.getString("KafkaProducerStep.Log.UsingKey", keyField));

				data.keyFieldNr = inputRowMeta.indexOfValue(keyField);

				if (data.keyFieldNr < 0) {
					logError(Messages.getString("KafkaProducerStep.Log.CouldntFindField", keyField)); //$NON-NLS-1$
					numErrors++;
				}
				if (!inputRowMeta.getValueMeta(data.keyFieldNr).isBinary()
						&& !inputRowMeta.getValueMeta(data.keyFieldNr).isString()) {
					logError(Messages.getString("KafkaProducerStep.Log.FieldNotValid", keyField)); //$NON-NLS-1$
					numErrors++;
				}
				data.keyIsString = inputRowMeta.getValueMeta(data.keyFieldNr).isString();
				data.keyFieldMeta = inputRowMeta.getValueMeta(data.keyFieldNr);

			} else {
				data.keyFieldNr = -1;
			}

			if (numErrors > 0) {
				setErrors(numErrors);
				stopAll();
				return false;
			}
		}
    // 二进制类型 尽可能使用base64 编码，这里暂时不修改了
		try {
			String message = null;

			if (data.messageIsString) {
				message = data.messageFieldMeta.getString(r[data.messageFieldNr]);
			} else {
				message = new String(data.messageFieldMeta.getBinary(r[data.messageFieldNr]));
			}
			String topic = environmentSubstitute(meta.getTopic());

			if (isRowLevel()) {
 				logDebug(Messages.getString("KafkaProducerStep.Log.SendingData", topic));
				logRowlevel(data.messageFieldMeta.getString(r[data.messageFieldNr]));
			}

			if (data.keyFieldNr < 0) {
				data.producer.send(new ProducerRecord<String, String>(topic, message));
			} else {
				String key = null;
				if (data.keyIsString) {
					key = data.keyFieldMeta.getString(r[data.keyFieldNr]);
				} else {
					key = new String(data.keyFieldMeta.getBinary(r[data.keyFieldNr]));
				}

				data.producer.send(new ProducerRecord<String, String>(topic, key, message));
			}

			incrementLinesOutput();
		} catch (KettleException e) {
			if (!getStepMeta().isDoingErrorHandling()) {
				logError(Messages.getString("KafkaProducerStep.ErrorInStepRunning", e.getMessage()));
				setErrors(1);
				stopAll();
				setOutputDone();
				return false;
			}
			putError(getInputRowMeta(), r, 1, e.toString(), null, getStepname());
		}
		return true;
	}

	public void stopRunning(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {

		KafkaProducerData data = (KafkaProducerData) sdi;
		if (data.producer != null) {
			data.producer.close();
			data.producer = null;
		}
		super.stopRunning(smi, sdi);
	}
}
