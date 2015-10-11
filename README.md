# HybridFlow

Static taint analysis of Android Hybrid App (Java + HTML).


## About

Increasing numbers of Android apps are ``hybrid'' (aka. hybrid apps), which contains both Java code and HTML code.

Java side and HTML side can interact with each other via WebView.
For example, the HTML in WebView can invoke Java interfaces, which are registered via `addJavascriptInterface`, .
For another example, the Java code can execute JS in HTML via `loadUrl`.
The data flow across Java and HTML are which we called hybrid data flow.

Existing Android taint analysis tools (`FlowDroid`, `AmanDroid`, `DroidSafe`, etc) focus on Java side data flow,
which are insufficient in handling the hybrid flow.
This tool (HybridFlow) aims to fix the gap between existing analysis tools and increasing numbers of hybrid apps.

## How does it work

HybridFlow analyse a hybrid app in three step:

1. *BuildBridge*. In this step, it performs a points-to analysis and a string analysis to
determine the hybrid bridges between Java and HTML.
This step produces a instrumented apk in `java` directory for Java side taint analysis,
and a `html` directory for HTML side taint analysis.
2. *RunTaintAnalysis*. In this step, it runs taint analysis for each side.
Currently, we use `FlowDroid` to run Java side taint analysis and extend WALA to run HTML side taint analysis.
3. *MergeTaintFlow*. This step merges the Java source-to-sink paths with the HTML source-to-sink paths generated in step 2,
according to the bridge generated in step 1.
The merging result is the hybrid source-to-sink flows.

## Installation

```
git clone XXX
mvn install
```

If things go well, the command will generate a executable jar file under `target` directory.

## Usage

The tool requires a apk file (which you want to analyze) and a sources and sinks definition as inputs.

You may also specify the Android SDK home which contains the proper version of android.jar.
For example, if the apk is targeted android 19, the android.jar file should appear in `sdk/platforms` directory.

Run:

```
java -jar hybridflow.jar -i webviewdemo.apk -d output -sdk $ANDROID_SDK_HOME$ -source_sink SourcesAndSinks.txt
```

If everything goes well, this command will lead to a full execution of all 3 steps,
a `AnalysisResult.md` file will be generated under `output` directory,
which contains the taint analysis result (source to sink paths).
You can also run each step separately using `-m` option.

### Example

The [`example`](example/) directory contains a running example of HybridFlow.
The [AnalysisResult.md](example/AnalysisResult.md) file is the generated report.

## Acknowledgement

