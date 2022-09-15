# jtodo [![Java CI with Maven](https://github.com/gaborbata/jtodo/workflows/Java%20CI%20with%20Maven/badge.svg)](https://github.com/gaborbata/jtodo/actions/workflows/maven.yml)

Experimental Java/JRuby front-end for [todo](https://github.com/gaborbata/todo) which supports tabs

![todo](https://raw.githubusercontent.com/gaborbata/todo/master/todo.gif)

## Usage

On most platforms the application can be run with the following command:

`java -jar jtodo-1.0-SNAPSHOT.jar`

You can find the latest binaries under [releases](https://github.com/gaborbata/jtodo/releases).

Requires Java 8 (or later).

## Shortcut keys

| Shortcut key | Description |
| ------------ | ----------- |
| `CTRL + T`   | Add new tab |
| `CTRL + W`   | Close tab   |
| `CTRL + Tab` | Change tab  |

## How to build

Maven or Gradle is required to build the application, with one the following commands:

`mvn clean package`, `gradle clean build`

### Dependencies

* [todo](https://github.com/gaborbata/todo) - todo list manager inspired by [todo.txt](http://todotxt.org) using the [jsonl](http://jsonlines.org) format
* [JRuby](https://github.com/jruby/jruby) - an implementation of the Ruby language on the JVM
* [FlatLaf](https://github.com/JFormDesigner/FlatLaf) - Flat Look and Feel
* [EvalEx](https://github.com/uklimaschewski/EvalEx) - mathematical expression evaluator
