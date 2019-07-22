// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.cli.config

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.io.IOException


internal class AngularJson {
  @JsonProperty("apps")
  val apps: List<AngularJsonProject> = ArrayList()

  @JsonProperty("projects")
  val projects: Map<String, AngularJsonProject> = HashMap()

  @JsonProperty("defaultProject")
  val defaultProject: String? = null
}

internal open class AngularJsonBuildOptionsBase {

  @JsonProperty("stylePreprocessorOptions")
  val stylePreprocessorOptions: AngularJsonStylePreprocessorOptions? = null

  @JsonProperty("index")
  val index: String? = null

  @JsonProperty("styles")
  @JsonDeserialize(using = StringOrObjectWithInputDeserializer::class)
  val styles: List<String>? = null

}

internal class AngularJsonProject : AngularJsonBuildOptionsBase() {
  @JsonProperty("name")
  val name: String? = null

  @JsonProperty("root")
  val rootPath: String? = null

  @JsonProperty("targets")
  @JsonAlias(value = ["architect"])
  val targets: AngularJsonTargets? = null

}

internal class AngularJsonTargets {
  @JsonProperty("build")
  val build: AngularJsonBuild? = null

  @JsonProperty("test")
  val test: AngularJsonTest? = null

  @JsonProperty("e2e")
  val e2e: AngularJsonE2E? = null

  @JsonProperty("lint")
  val lint: AngularJsonLint? = null
}

internal class AngularJsonE2E {
  @JsonProperty("options")
  val options: AngularJsonE2EOptions? = null
}

internal class AngularJsonE2EOptions {
  @JsonProperty("protractorConfig")
  val protractorConfig: String? = null
}

internal class AngularJsonTest {
  @JsonProperty("options")
  val options: AngularJsonTestOptions? = null
}

internal class AngularJsonTestOptions {
  @JsonProperty("karmaConfig")
  val karmaConfig: String? = null
}

internal class AngularJsonBuild {
  @JsonProperty("options")
  val options: AngularJsonBuildOptions? = null
}

internal class AngularJsonBuildOptions : AngularJsonBuildOptionsBase()

internal class AngularJsonStylePreprocessorOptions {
  @JsonProperty("includePaths")
  val includePaths: List<String> = ArrayList()
}

internal class AngularJsonLint {
  @JsonProperty("options")
  val options: AngularJsonLintOptions? = null

  @JsonProperty("configurations")
  val configurations: Map<String, AngularJsonLintOptions> = HashMap()
}

internal class AngularJsonLintOptions {
  @JsonProperty
  @JsonDeserialize(using = StringOrStringArrayDeserializer::class)
  val tsConfig: List<String> = emptyList()

  @JsonProperty("tslintConfig")
  val tsLintConfig: String? = null

  @JsonProperty("files")
  val files: List<String> = emptyList()

  @JsonProperty("exclude")
  val exclude: List<String> = emptyList()
}

private class StringOrObjectWithInputDeserializer : JsonDeserializer<List<String>>() {

  @Throws(IOException::class)
  override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): List<String> {
    val files = mutableListOf<String>()
    if (jsonParser.currentToken === JsonToken.START_ARRAY) {
      while (jsonParser.nextToken() !== JsonToken.END_ARRAY) {
        when (jsonParser.currentToken) {
          JsonToken.START_OBJECT -> while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
            assert(jsonParser.currentToken === JsonToken.FIELD_NAME)
            val propName = jsonParser.currentName
            jsonParser.nextToken()
            if (propName == "input") {
              files.add(jsonParser.valueAsString)
            }
            else {
              jsonParser.skipChildren()
            }
          }
          JsonToken.VALUE_STRING -> files.add(jsonParser.valueAsString)
          else -> deserializationContext.handleUnexpectedToken(String::class.java, jsonParser)
        }
      }
    }
    else {
      deserializationContext.handleUnexpectedToken(List::class.java, jsonParser)
    }
    return files
  }
}

private class StringOrStringArrayDeserializer : JsonDeserializer<List<String>>() {

  override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): List<String> {
    val items = mutableListOf<String>()
    when (jsonParser.currentToken) {
      JsonToken.START_ARRAY ->
        while (jsonParser.nextToken() !== JsonToken.END_ARRAY) {
          if (jsonParser.currentToken === JsonToken.VALUE_STRING) {
            items.add(jsonParser.valueAsString)
          }
          else {
            deserializationContext.handleUnexpectedToken(String::class.java, jsonParser)
          }
        }
      JsonToken.VALUE_STRING -> items.add(jsonParser.valueAsString)
      else -> deserializationContext.handleUnexpectedToken(String::class.java, jsonParser)
    }
    return items
  }

}
