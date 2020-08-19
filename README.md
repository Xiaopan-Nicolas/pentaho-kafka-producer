pentaho-kafka-producer
======================

Apache Kafka producer step plug-in for Pentaho Kettle.

[![Build Status](https://travis-ci.org/RuckusWirelessIL/pentaho-kafka-producer.png)](https://travis-ci.org/RuckusWirelessIL/pentaho-kafka-producer)


### Screenshots ###

![Using Apache Kafka Producer in Kettle](https://raw.github.com/RuckusWirelessIL/pentaho-kafka-producer/master/doc/example.png)


### Apache Kafka Compatibility ###

The producer depends on Apache Kafka 0.8.1.1, which means that the broker must be of 0.8.x version or later.


### Installation ###

1. Download ```pentaho-kafka-producer``` Zip archive from [latest release page](https://github.com/RuckusWirelessIL/pentaho-kafka-producer/releases/latest).
2. Extract downloaded archive into *plugins/steps* directory of your Pentaho Data Integration distribution.


### Building from source code ###

```
mvn clean package
```
### 华为kerberos认证

使用步骤：

1. kettle安装目录，创建conf文件夹

   ```
   ├── krb5.conf
   ├── user.jaas.conf
   └── user.keytab
   ```

2. Spoon.bat或者Spoon.sh修改OPT变量增加以下部分，地址修改为自己kettle位置
   ```
   OPT="$OPT -Djava.security.auth.login.config=/Users/xiao/dev/kettle/conf/user.jaas.conf -Djava.security.krb5.conf=/Users/xiao/dev/kettle/conf/krb5.conf"
   ```

3. user.jaas.conf文件内容如下，按照实际进行修改

   ```
   EsClient{
   com.sun.security.auth.module.Krb5LoginModule required
   useKeyTab=true
   keyTab="/etc/user.keytab"
   principal="chinaoly"
   useTicketCache=false
   storeKey=true
   debug=true;
   };
   Client{
   com.sun.security.auth.module.Krb5LoginModule required
   useKeyTab=true
   keyTab="/etc/user.keytab"
   principal="chinaoly"
   useTicketCache=false
   storeKey=true
   debug=true;
   };
   StormClient{
   com.sun.security.auth.module.Krb5LoginModule required
   useKeyTab=true
   keyTab="/etc/user.keytab"
   principal="chinaoly"
   useTicketCache=false
   storeKey=true
   debug=true;
   };
   KafkaClient{
   com.sun.security.auth.module.Krb5LoginModule required
   useKeyTab=true
   keyTab="/etc/user.keytab"
   principal="chinaoly"
   useTicketCache=false
   storeKey=true
   debug=true;
   };
   ```

4. 使用kerberos认证必须设置`isSecureMode` 为`true`，除了`bootstrap.servers`以外其他默认即可。