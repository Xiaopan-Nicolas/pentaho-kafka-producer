package com.ruckuswireless.pentaho.kafka.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
 * Holds data processed by this step
 * 
 * @author Michael
 */
public class KafkaProducerData extends BaseStepData implements StepDataInterface {

	KafkaProducer<String, String> producer;
	RowMetaInterface outputRowMeta;
	int messageFieldNr;
	int keyFieldNr;
	boolean messageIsString;
	boolean keyIsString;
	ValueMetaInterface messageFieldMeta;
	ValueMetaInterface keyFieldMeta;
}
