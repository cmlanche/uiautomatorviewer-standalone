### 说明
这个工程是[谷歌官方UIAutomatorViewer](https://android.googlesource.com/platform/tools/swt/+/refs/heads/android10-release/uiautomatorviewer/)改造而来，用来抓取UIAutomator控件的工具，支持独立运行，但因为其依赖于swt，所以需要在mac和windows下分别编译。

### 打包
mvn clean package

### 执行
java -XstartOnFirstThread -jar target/uiautomatorviewer-standalone-1.0-SNAPSHOT-all.jar

### 源码来源
源码来自[谷歌官方](https://android.googlesource.com/platform/tools/swt/+/refs/heads/android10-release/uiautomatorviewer/)

查阅依赖后找到相关库：
1. swt：https://download.eclipse.org/eclipse/downloads/drops4/I20200830-1800/#SWT
2. jface: 下载[RCP Runtime Binary](https://download.eclipse.org/eclipse/downloads/drops4/I20200830-1800/)，解压后查找org.eclipse.core.commands_{version}.jar、org.eclipse.equinox.common_{version}.jar、org.eclipse.jface_{version}.jar
3. 额外还包括其他依赖：
```xml
<dependency>
    <groupId>com.android.tools.ddms</groupId>
    <artifactId>ddmlib</artifactId>
    <version>25.3.0</version>
</dependency>
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.7</version>
</dependency>
```