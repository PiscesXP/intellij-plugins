// Copyright 2000-2018 JetBrains s.r.o.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.jetbrains.vuejs.language

import com.intellij.lang.ecmascript6.psi.JSClassExpression
import com.intellij.lang.javascript.JSTestUtils
import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.ecma6.TypeScriptFunction
import com.intellij.lang.javascript.psi.ecma6.TypeScriptPropertySignature
import com.intellij.lang.javascript.psi.impl.JSReferenceExpressionImpl
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.Trinity
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.intellij.util.ThrowableRunnable
import junit.framework.TestCase
import org.jetbrains.vuejs.codeInsight.VueJSSpecificHandlersFactory

/**
 * @author Irina.Chernushina on 7/28/2017.
 */
class VueResolveTest : LightPlatformCodeInsightFixtureTestCase() {
  override fun getTestDataPath(): String = PathManager.getHomePath() + "/contrib/vuejs/vuejs-tests/testData/resolve/"

  fun testResolveInjectionToPropInObject() {
    myFixture.configureByText("ResolveToPropInObject.vue", """
<template>
    <div class="list">
        <ul>
            <li>
                {{ <caret>message25620 }}
            </li>
        </ul>
    </div>
</template>

<script>
  export default {
    name: 'list',
    props: {message25620: {}}
  }
  let message25620 = 111;
</script>""")
    val reference =  myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
    TestCase.assertNotNull(reference)
    TestCase.assertTrue(reference is JSReferenceExpression)
    val resolver = VueJSSpecificHandlersFactory().createReferenceExpressionResolver(
      reference as JSReferenceExpressionImpl, true)
    val results = resolver.resolve(reference, false)
    TestCase.assertEquals(1, results.size)
    TestCase.assertTrue(results[0].element!! is JSProperty)
    TestCase.assertEquals("message25620", (results[0].element!! as JSProperty).name)
  }

  fun testResolveUsageInAttributeToPropInArray() {
    myFixture.configureByText("ResolveToPropInObject.vue", """
<template>
  <list25620 v-text="'prefix' + <caret>message25620Arr + 'postfix'">
  Text
  </list25620>
</template>

<script>
  export default {
    name: 'list25620',
    props: ['message25620Arr']
  }
</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    TestCase.assertTrue(reference is JSReferenceExpression)
    val resolver = VueJSSpecificHandlersFactory().createReferenceExpressionResolver(
      reference as JSReferenceExpressionImpl, true)
    val results = resolver.resolve(reference, false)
    TestCase.assertEquals(1, results.size)
    val literal = results[0].element!!
    TestCase.assertTrue(literal is JSLiteralExpression)
    TestCase.assertTrue(literal.parent is JSArrayLiteralExpression)
    TestCase.assertEquals("'message25620Arr'", literal.text)
  }

  fun testResolveAttributeInPascalCaseUsageInPropsArray() {
    myFixture.configureByText("ResolveAttributeInPascalCaseUsageInPropsArray.vue", """
<template>
  <list25620 <caret>PascalCase">
  Text
  </list25620>
</template>

<script>
  export default {
    name: 'list25620',
    props: ['pascalCase']
  }
  let message25620 = 111;
</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val literal = reference!!.resolve()
    TestCase.assertTrue(literal is JSLiteralExpression)
    TestCase.assertTrue(literal!!.parent is JSArrayLiteralExpression)
    TestCase.assertEquals("'pascalCase'", literal.text)
  }

  fun testResolveIntoComputedProperty() {
    myFixture.configureByText("ResolveIntoComputedProperty.vue", """
<template>
{{<caret>TestRight}}
</template>
<script>
export default {
  name: 'childComp',
  props: {'myMessage': {}},
  computed: {
    testWrong: 111,
    testRight: function() {}
  }
}
</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertEquals("testRight", (property as JSProperty).name)
  }

  fun testResolveIntoComputedES6FunctionProperty() {
    myFixture.configureByText("ResolveIntoComputedES6FunctionProperty.vue", """
<template>
{{<caret>TestRight}}
</template>
<script>
export default {
  name: 'childComp',
  props: {'myMessage': {}},
  computed: {
    testWrong: 111,
    testRight() {}
  }
}
</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertEquals("testRight", (property as JSProperty).name)
  }

  fun testResolveIntoMethodsFromBoundAttributes() {
    myFixture.configureByText("child.vue", """
<template>
</template>
<script>
export default {
  name: 'childComp',
  props: {'myMessage': {}},
  methods: {
    reverseMessage() {
      return this.myMessage.reverse()
    }
  }
}
</script>""")
    myFixture.configureByText("ResolveIntoMethodsFromBoundAttributes.vue", """
<template>
    <child-comp v-bind:my-message="me215t<caret>hod"></child-comp>
</template>
<script>
import ChildComp from 'child.vue'
export default {
  components: {ChildComp},
  name: 'parent',
  methods: {
    me215thod: function () {
      return 'something!'
    }
  }
}
</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertEquals("me215thod", (property as JSProperty).name)
  }

  fun testResolveLocallyInsideComponentPropsArray() {
    myFixture.configureByText("ResolveLocallyInsideComponentPropsArray.vue", """
<script>
export default {
  name: 'parent',
  props: ['parentMsg', 'parentSize'],
  computed: {
    normalizedSize: function () {
      return this.<caret>parentMsg.trim().toLowerCase()
    }
  }
}</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val literal = reference!!.resolve()
    TestCase.assertTrue(literal is JSLiteralExpression)
    TestCase.assertTrue((literal as JSLiteralExpression).isQuotedLiteral)
    TestCase.assertEquals("'parentMsg'", literal.text)
  }

  fun testResolveLocallyInsideComponentPropsArrayRefVariant() {
    myFixture.configureByText("ResolveLocallyInsideComponentPropsArrayRefVariant.vue", """
<script>
let props = ['parentMsg', 'parentSize'];
export default {
  name: 'parent',
  props: props,
  computed: {
    normalizedSize: function () {
      return this.<caret>parentMsg.trim().toLowerCase()
    }
  }
}</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val literal = reference!!.resolve()
    TestCase.assertTrue(literal is JSLiteralExpression)
    TestCase.assertTrue((literal as JSLiteralExpression).isQuotedLiteral)
    TestCase.assertEquals("'parentMsg'", literal.text)
  }

  fun testResolveLocallyInsideComponentArrayFunctionInsideExport() {
    JSTestUtils.testES6<Exception>(myFixture.project, {
      myFixture.configureByText("ResolveLocallyInsideComponentArrayFunctionInsideExport.vue", """
<script>
let props = ['parentMsg'];

export default {
  name: 'parent',
  props: props,
  methods: {
    oneMethod: () => {
      return this.<caret>parentMsg * 3;
    }
  }
}</script>""")
      val reference = myFixture.getReferenceAtCaretPosition()
      TestCase.assertNotNull(reference)
      val literal = reference!!.resolve()
      TestCase.assertTrue(literal is JSLiteralExpression)
      TestCase.assertTrue((literal as JSLiteralExpression).isQuotedLiteral)
      TestCase.assertEquals("'parentMsg'", literal.text)
    })
  }

  fun testResolveLocallyInsideComponent() {

    doTestResolveLocallyInsideComponent("""
<script>
export default {
  name: 'parent',
  props: {parentMsg: {}, parentSize: {}},
  computed: {
    normalizedSize: function () {
      return this.<caret>parentMsg.trim().toLowerCase()
    }
  }
}</script>
""", "parentMsg")

    doTestResolveLocallyInsideComponent("""
<script>
let props = {parentMsg: {}, parentSize: {}};
export default {
  name: 'parent',
  props: props,
  computed: {
    normalizedSize: function () {
      return this.<caret>parentMsg.trim().toLowerCase()
    }
  }
}</script>
""", "parentMsg")

    doTestResolveLocallyInsideComponent("""
<script>
let props = {parentMsg: {}, parentSize: {}};
export default {
  name: 'parent',
  props: props,
  methods: {
    normalizedSize() {
      return this.<caret>parentMsg.trim().toLowerCase()
    }
  }
}</script>
""", "parentMsg")

    doTestResolveLocallyInsideComponent("""
<script>
let props = {parentMsg: {}, parentSize: {}};
let methods = {
    wise() {
      return this.<caret>normalizedSize() / 2;
    }
  };
let computedProps = {
    normalizedSize() {
      return this.parentMsg.trim().toLowerCase()
    }
  };
export default {
  name: 'parent',
  props: props,
  computedProps: computedProps,
  methods: methods
}</script>
""", "normalizedSize")

    doTestResolveLocallyInsideComponent("""
<script>
let props = {parentMsg: {}, parentSize: {}};

function wouldBeUsedLater() {
  return this.<caret>parentMsg * 3;
}

export default {
  name: 'parent',
  props: props,
}</script>
""", "parentMsg")

    doTestResolveLocallyInsideComponent("""
<script>
let props = ['parentMsg'];

let wouldBeUsedLater = () => {
  return this.<caret>parentMsg * 3;
}

export default {
  name: 'parent',
  props: props,
}</script>
""", null)

    doTestResolveLocallyInsideComponent("""
<template>{{<caret>groceryList}}</template>
<script>
let props = ['parentMsg'];

export default {
  name: 'parent',
  props: props,
  data: {
    groceryList: {}
  }
}</script>
""", "groceryList")

    doTestResolveLocallyInsideComponent("""
<script>
let props = ['parentMsg'];

export default {
  name: 'parent',
  props: props,
  methods: {
    callMum() {
      return this.<caret>groceryList;
    }
  }
  data: {
    groceryList: {}
  }
}</script>
""", "groceryList")

    doTestResolveLocallyInsideComponent("""
<template>{{<caret>groceryList}}</template>
<script>
let props = ['parentMsg'];

export default {
  name: 'parent',
  props: props,
  data: () => ({
    groceryList: {}
  })
}</script>
""", "groceryList")

    doTestResolveLocallyInsideComponent("""
<template>{{<caret>groceryList}}</template>
<script>
let props = ['parentMsg'];

export default {
  name: 'parent',
  props: props,
  data:
    () => {
            return {
              groceryList: {}
            }
          }
}</script>
""", "groceryList")

    doTestResolveLocallyInsideComponent("""
<template>{{groceryList.<caret>carrot}}</template>
<script>
let props = ['parentMsg'];

export default {
  name: 'parent',
  props: props,
  data:
    function () {
            return {
              groceryList: {
                carrot: {}
              }
            }
          }
}</script>
""", "carrot")
}

  private fun doTestResolveLocallyInsideComponent(text: String, expectedPropertyName: String?) {
    myFixture.configureByText("ResolveLocallyInsideComponent.vue", text)
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    if (expectedPropertyName == null) {
      TestCase.assertNull(property)
    } else {
      TestCase.assertTrue(property is JSProperty)
      TestCase.assertEquals(expectedPropertyName, (property as JSProperty).name)
    }
  }

  fun testIntoVForVar() {
    myFixture.configureByText("IntoVForVar.vue", """
<template>
  <ul>
    <li v-for="item in items">
      {{ <caret>item.message }}
    </li>
  </ul>
</template>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val variable = reference!!.resolve()
    TestCase.assertNotNull(variable)
    TestCase.assertTrue(variable!!.parent.parent is VueVForExpression)
  }

  fun testVForDetailsResolve() {
    myFixture.configureByText("IntoVForDetailsResolve.vue", """
<template>
  <ul>
    <li v-for="item in items">
      {{ item.<caret>message }}
    </li>
  </ul>
</template>
<script>
  export default {
    name: 'v-for-test',
    data: {
      items: [
        { message: 'Foo' }
      ]
    }
  }
</script>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val part = reference!!.resolve()
    TestCase.assertNotNull(part)
    TestCase.assertTrue(part is JSProperty)
    TestCase.assertTrue(part!!.parent is JSObjectLiteralExpression)
  }

  fun testVForIteratedExpressionResolve() {
    myFixture.configureByText("VForIteratedExpressionResolve.vue", """
<template>
  <ul>
    <li v-for="item in <caret>items">
      {{ item.message }}
    </li>
  </ul>
</template>
<script>
  export default {
    name: 'v-for-test',
    data: {
      items: [
        { message: 'Foo' }
      ]
    }
  }
</script>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val part = reference!!.resolve()
    TestCase.assertNotNull(part)
    TestCase.assertTrue(part is JSProperty)
    TestCase.assertTrue(part!!.parent is JSObjectLiteralExpression)
  }

  fun testIntoVForVarInPug() {
    myFixture.configureByText("IntoVForVarInPug.vue", """
<template lang="pug">
  ul
    li(v-for="item in items") {{ <caret>item.message }}
</template>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val variable = reference!!.resolve()
    TestCase.assertNotNull(variable)
    TestCase.assertTrue(variable!!.parent.parent is VueVForExpression)
  }

  fun testVForDetailsResolveInPug() {
    myFixture.configureByText("IntoVForDetailsResolveInPug.vue", """
<template lang="pug">
  ul
    li(v-for="item in items") {{ item.<caret>message }}
</template>
<script>
  export default {
    name: 'v-for-test',
    data: {
      items: [
        { message: 'Foo' }
      ]
    }
  }
</script>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val part = reference!!.resolve()
    TestCase.assertNotNull(part)
    TestCase.assertTrue(part is JSProperty)
    TestCase.assertTrue(part!!.parent is JSObjectLiteralExpression)
  }

  fun testVForIteratedExpressionResolveInPug() {
    myFixture.configureByText("VForIteratedExpressionResolveInPug.vue", """
<template lang="pug">
  ul
    li(v-for="item in <caret>itemsPP") {{ item.message }}
</template>
<script>
  export default {
    name: 'v-for-test',
    data: {
      itemsPP: [
        { message: 'Foo' }
      ]
    }
  }
</script>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val part = reference!!.resolve()
    TestCase.assertNotNull(part)
    TestCase.assertTrue(part is JSProperty)
    TestCase.assertTrue(part!!.parent is JSObjectLiteralExpression)
  }

  fun testIntoVForVarInHtml() {
    myFixture.configureByText("a.vue", "")
    myFixture.configureByText("IntoVForVarInHtml.html", """
<html>
  <ul>
    <li v-for="itemHtml in itemsHtml">
      {{ <caret>itemHtml.message }}
    </li>
  </ul>
</html>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val variable = reference!!.resolve()
    TestCase.assertNotNull(variable)
    TestCase.assertTrue(variable!!.parent.parent is VueVForExpression)
  }

  fun testKeyIntoForResolve() {
    myFixture.configureByText("KeyIntoForResolve.vue", """
<template>
  <li v-for="(item1, index1) of items1" :key="<caret>item1" v-if="item1 > 0">
    {{ parentMessage1 }} - {{ index1 }} - {{ item1.message1 }}
  </li>
</template>
<script>
  export default {
    data: {
      parentMessage1: 'Parent',
      items1: [
        { message1: 'Foo' },
        { message1: 'Bar' }
      ]
    }
  }
</script>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val variable = reference!!.resolve()
    TestCase.assertNotNull(variable)
    TestCase.assertTrue(variable!!.parent is JSVarStatement)
    TestCase.assertTrue(variable.parent.parent is JSParenthesizedExpression)
    TestCase.assertTrue(variable.parent.parent.parent is VueVForExpression)
  }

  fun testVIfIntoForResolve() {
    myFixture.configureByText("VIfIntoForResolve.vue", """
<template>
  <li v-for="(item1, index1) in items1" :key="item1" v-if="<caret>item1 > 0">
    {{ parentMessage1 }} - {{ index1 }} - {{ item1.message1 }}
  </li>
</template>
<script>
  export default {
    data: {
      parentMessage1: 'Parent',
      items1: [
        { message1: 'Foo' },
        { message1: 'Bar' }
      ]
    }
  }
</script>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val variable = reference!!.resolve()
    TestCase.assertNotNull(variable)
    TestCase.assertTrue(variable!!.parent is JSVarStatement)
    TestCase.assertTrue(variable.parent.parent is JSParenthesizedExpression)
    TestCase.assertTrue(variable.parent.parent.parent is VueVForExpression)
  }

  fun testKeyIntoForResolveHtml() {
    myFixture.configureByText("a.vue", "")
    myFixture.configureByText("KeyIntoForResolveHtml.html", """
<html>
  <li id="id123" v-for="(item1, index1) in items1" :key="<caret>item1" v-if="item1 > 0">
    {{ parentMessage1 }} - {{ index1 }} - {{ item1.message1 }}
  </li>
</html>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val variable = reference!!.resolve()
    TestCase.assertNotNull(variable)
    TestCase.assertTrue(variable!!.parent is JSVarStatement)
    TestCase.assertTrue(variable.parent.parent is JSParenthesizedExpression)
    TestCase.assertTrue(variable.parent.parent.parent is VueVForExpression)
  }

  fun testResolveByMountedVueInstanceInData() {
    myFixture.configureByText("a.vue", "")
    myFixture.configureByText("ResolveByMountedVueInstanceInData.js", """
new Vue({
  el: '#ResolveByMountedVueInstanceInData',
  data: {
    messageToFind: 'Parent'
  }
})
""")
    myFixture.configureByText("ResolveByMountedVueInstanceInData.html", """
<!DOCTYPE html>
<html lang="en">
<body>
<ul id="ResolveByMountedVueInstanceInData">
  {{ <caret>messageToFind }}
</ul>
</body>
</html>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertNotNull(property)
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertTrue(property!!.parent.parent is JSProperty)
    TestCase.assertEquals("data", (property.parent.parent as JSProperty).name)
  }

  fun testResolveByMountedVueInstanceInProps() {
    myFixture.configureByText("a.vue", "")
    myFixture.configureByText("ResolveByMountedVueInstanceInProps.js", """
new Vue({
  el: '#ResolveByMountedVueInstanceInProps',
  props: ['compProp']
})
""")
    myFixture.configureByText("ResolveByMountedVueInstanceInProps.html", """
<!DOCTYPE html>
<html lang="en">
<body>
<ul id="ResolveByMountedVueInstanceInProps">
  {{ <caret>compProp }}
</ul>
</body>
</html>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val arrayItem = reference!!.resolve()
    TestCase.assertNotNull(arrayItem)
    TestCase.assertTrue(arrayItem is JSLiteralExpression)
    TestCase.assertTrue(arrayItem!!.parent.parent is JSProperty)
    TestCase.assertEquals("props", (arrayItem.parent.parent as JSProperty).name)
  }

  fun testResolveVForIterableByMountedVueInstance() {
    myFixture.configureByText("a.vue", "")
    myFixture.configureByText("ResolveVForIterableByMountedVueInstance.js", """
new Vue({
  el: '#ResolveVForIterableByMountedVueInstance',
  data: {
    parentMessage: 'Parent',
    mountedItems: [
      { message2233: 'Foo' },
      { message2233: 'Bar' }
    ]
  }
})
""")
    myFixture.configureByText("ResolveVForIterableByMountedVueInstance.html", """
<!DOCTYPE html>
<html lang="en">
<body>
<ul id="ResolveVForIterableByMountedVueInstance">
  <li v-for="(item, index) in <caret>mountedItems">
    {{parentMessage }} - {{ index }} - {{item.message2233 }}
  </li>
</ul>
</body>
</html>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertNotNull(property)
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertTrue(property!!.parent.parent is JSProperty)
    TestCase.assertEquals("data", (property.parent.parent as JSProperty).name)
  }

  fun testKeyIntoForResolvePug() {
    myFixture.configureByText("KeyIntoForResolvePug.vue", """
<template lang="pug">
  ul
    li(id="id123" v-for="(item123, index1) in items1", :key="<caret>item123") {{ parentMessage1 }}
</template>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val variable = reference!!.resolve()
    TestCase.assertNotNull(variable)
    TestCase.assertTrue(variable!!.parent is JSVarStatement)
    TestCase.assertTrue(variable.parent.parent is JSParenthesizedExpression)
    TestCase.assertTrue(variable.parent.parent.parent is VueVForExpression)
  }

  fun testResolveForRenamedGlobalComponent() {
    myFixture.configureByText("libComponent.vue", """
<template>text here</template>
<script>
  export default {
    name: 'libComponent',
    props: ['libComponentProp']
  }
</script>
""")
    myFixture.configureByText("main.js", """
import LibComponent from "./libComponent"
Vue.component('renamed-component', LibComponent)
""")
    myFixture.configureByText("CompleteWithoutImportForRenamedGlobalComponent.vue", """
<template>
<renamed-component <caret>lib-component-prop=1></renamed-component>
</template>
<script>
export default {
}
</script>
""")

    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val literal = reference!!.resolve()
    TestCase.assertNotNull(literal)
    TestCase.assertTrue(literal is JSLiteralExpression)
    TestCase.assertEquals("'libComponentProp'", literal!!.text)
    TestCase.assertTrue(literal.parent is JSArrayLiteralExpression)
    TestCase.assertEquals("props", (literal.parent.parent as JSProperty).name)
  }

  fun testLocalQualifiedNameOfGlobalComponent() {
    myFixture.configureByText("LocalQualifiedNameOfGlobalComponent.js", """
      let CompDef = {
        props: {
          kuku: {}
        }
      };

      Vue.component('complex-ref', CompDef);
    """.trimIndent())
    myFixture.configureByText("LocalQualifiedNameOfGlobalComponent.vue", """
      <template>
        <complex-ref <caret>kuku="e23"></complex-ref>
      </template>
    """.trimIndent())

    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertNotNull(property)
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertEquals("kuku", (property as JSProperty).name)
    TestCase.assertTrue(property.parent.parent is JSProperty)
    TestCase.assertEquals("props", (property.parent.parent as JSProperty).name)
  }

  fun testResolveVueRouterComponents() {
    myFixture.configureByFile("vue-router.js")
    myFixture.configureByText("ResolveVueRouterComponents.vue", """
      <template>
        <router-link <caret>to="/post"></router-link>
      </template>
    """.trimIndent())

    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertNotNull(property)
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertEquals("to", (property as JSProperty).name)
    TestCase.assertTrue(property.parent.parent is JSProperty)
    TestCase.assertEquals("props", (property.parent.parent as JSProperty).name)
  }

  fun testResolveIntoGlobalComponentInLocalVar() {
    myFixture.configureByText("ResolveIntoGlobalComponentInLocalVarComponent.js", """
(function(a, b, c) {/* ... */}
(
  a,b,
  function() {
      let CompDefIFFE = {
        props: {
          from: {}
        }
      };

      function install() {
        Vue.component('iffe-comp', CompDefIFFE);
      }
  }
))
""")
    myFixture.configureByText("ResolveIntoGlobalComponentInLocalVarComponent.vue", """
      <template>
        <iffe-comp <caret>from="e23"></complex-ref>
      </template>
""")

    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertNotNull(property)
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertEquals("from", (property as JSProperty).name)
    TestCase.assertTrue(property.parent.parent is JSProperty)
    TestCase.assertEquals("props", (property.parent.parent as JSProperty).name)
  }

  fun testGlobalComponentNameInReference() {
    myFixture.configureByText("WiseComp.vue",
"""
<script>export default { name: 'wise-comp', props: {} }</script>
""")
    myFixture.configureByText("register.es6",
"""
import WiseComp from 'WiseComp'
const alias = 'wise-comp-alias'
Vue.component(alias, WiseComp)
""")
    myFixture.configureByText("use.vue",
"""
<template><<caret>wise-comp-alias</template>
""")
    doResolveAliasIntoLibraryComponent("wise-comp", "WiseComp.vue")
  }

  private fun doResolveAliasIntoLibraryComponent(compName: String, fileName: String) {
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val target = reference!!.resolve()
    TestCase.assertNotNull(target)
    TestCase.assertEquals(fileName, target!!.containingFile.name)
    TestCase.assertTrue(target.parent is JSObjectLiteralExpression)
    TestCase.assertEquals(compName, (target as JSImplicitElement).name)
  }

  fun testGlobalComponentLiteral() {
    myFixture.configureByText("index.js", """
Vue.component('global-comp-literal', {
  props: {
    insideGlobalCompLiteral: {}
  }
});
""")
    myFixture.configureByText("GlobalComponentLiteral.vue", """
<template>
  <global-comp-literal <caret>inside-global-comp-literal=222></global-comp-literal>
</template>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertNotNull(property)
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertEquals("insideGlobalCompLiteral", (property as JSProperty).name)
    TestCase.assertTrue(property.parent.parent is JSProperty)
    TestCase.assertEquals("props", (property.parent.parent as JSProperty).name)
  }

  fun testLocalPropsInArrayInCompAttrsAndWithKebabCaseAlso() {
    JSTestUtils.testES6(myFixture.project, ThrowableRunnable<Exception> {
      myFixture.configureByText("LocalPropsInArrayInCompAttrsAndWithKebabCaseAlso.vue",
                                """
<template>
    <div id="app">
        <camelCase <caret>one-two="test" three-four=1></camelCase>
    </div>
</template>
<script>
    export default {
      name: 'camelCase',
      props: ['oneTwo']
    }
</script>
""")
      myFixture.checkHighlighting()
      myFixture.doHighlighting()
      val reference = myFixture.getReferenceAtCaretPosition()
      TestCase.assertNotNull(reference)
      val literal = reference!!.resolve()
      TestCase.assertNotNull(literal)
      TestCase.assertTrue(literal is JSLiteralExpression)
      TestCase.assertEquals("oneTwo", (literal as JSLiteralExpression).stringValue)
      TestCase.assertTrue(literal.parent is JSArrayLiteralExpression)
    })
  }

  fun testLocalPropsInArrayInCompAttrsRef() {
    JSTestUtils.testES6(myFixture.project, ThrowableRunnable<Exception> {
      myFixture.configureByText("LocalPropsInArrayInCompAttrsRef.vue",
                                """
<template>
    <div id="app">
        <camelCase <caret>one-two="test" three-four=1></camelCase>
    </div>
</template>
<script>
const props = ['oneTwo']
    export default {
      name: 'camelCase',
      props: props
    }
</script>
""")
      myFixture.doHighlighting()
      val reference = myFixture.getReferenceAtCaretPosition()
      TestCase.assertNotNull(reference)
      val literal = reference!!.resolve()
      TestCase.assertNotNull(literal)
      TestCase.assertTrue(literal is JSLiteralExpression)
      TestCase.assertEquals("oneTwo", (literal as JSLiteralExpression).stringValue)
      TestCase.assertTrue(literal.parent is JSArrayLiteralExpression)
    })
  }


  fun testImportedComponentPropsInCompAttrsAsArray() {
    JSTestUtils.testES6(myFixture.project, ThrowableRunnable<Exception> {
      myFixture.configureByText("compUI.vue", """
<script>
    export default {
        name: 'compUI',
        props: ['seeMe']
    }
</script>
""")
      myFixture.configureByText("ImportedComponentPropsAsArray.vue", """
<template>
    <div id="app">
        <comp-u-i <caret>see-me="12345" butNotThis="112"></comp-u-i>
    </div>
</template>
<script>
    import CompUI from 'compUI.vue'
    export default {
      components: {CompUI}
    }
</script>
""")
      myFixture.checkHighlighting()
    })
    myFixture.doHighlighting()
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val literal = reference!!.resolve()
    TestCase.assertNotNull(literal)
    TestCase.assertTrue(literal is JSLiteralExpression)
    TestCase.assertEquals("seeMe", (literal as JSLiteralExpression).stringValue)
    TestCase.assertEquals("compUI.vue", literal.containingFile.name)
    TestCase.assertTrue(literal.parent is JSArrayLiteralExpression)
  }

  fun testImportedComponentPropsInCompAttrsObjectRef() {
    JSTestUtils.testES6(myFixture.project, ThrowableRunnable<Exception> {
      myFixture.configureByText("compUI.vue", """
<script>
const props = {seeMe: {}}
    export default {
        name: 'compUI',
        props: props
    }
</script>
""")
      myFixture.configureByText("ImportedComponentPropsAsObjectRef.vue", """
<template>
    <div id="app">
        <comp-u-i <caret>see-me="12345" ></comp-u-i>
    </div>
</template>
<script>
    import CompUI from 'compUI.vue'
    export default {
      components: {CompUI}
    }
</script>
""")
      myFixture.checkHighlighting()
      val reference = myFixture.getReferenceAtCaretPosition()
      TestCase.assertNotNull(reference)
      val property = reference!!.resolve()
      TestCase.assertNotNull(property)
      TestCase.assertTrue(property is JSProperty)
      TestCase.assertEquals("seeMe", (property as JSProperty).name)
      TestCase.assertEquals("compUI.vue", property.containingFile.name)
    })
  }

  fun testImportedComponentPropsInCompAttrsAsObject() {
    JSTestUtils.testES6(myFixture.project, ThrowableRunnable<Exception> {
      myFixture.configureByText("compUI.vue", """
<script>
    export default {
        name: 'compUI',
        props: {
          seeMe: {}
        }
    }
</script>
""")
      myFixture.configureByText("ImportedComponentPropsAsObject.vue", """
<template>
    <div id="app">
        <comp-u-i <caret>see-me="12345" butNotThis="112"></comp-u-i>
    </div>
</template>
<script>
    import CompUI from 'compUI.vue'
    export default {
      components: {CompUI}
    }
</script>
""")
      myFixture.checkHighlighting()
      val reference = myFixture.getReferenceAtCaretPosition()
      TestCase.assertNotNull(reference)
      val property = reference!!.resolve()
      TestCase.assertNotNull(property)
      TestCase.assertTrue(property is JSProperty)
      TestCase.assertTrue(property!!.parent.parent is JSProperty)
      TestCase.assertEquals("props", (property.parent.parent as JSProperty).name)
      TestCase.assertEquals("seeMe", (property as JSProperty).name)
      TestCase.assertEquals("compUI.vue", property.containingFile.name)
    })
  }

  fun testResolveMixinProp() {
    myFixture.configureByText("MixinWithProp.vue", """
<script>
    export default {
        props: {
            mixinProp:  {
                type: String
            }
        },
        methods: {
            helloFromMixin: function () {
                console.log('hello from mixin!')
            }
        }
    }
</script>
""")
    myFixture.configureByText("CompWithMixin.vue", """
<template>
    <div>
        <div>{{ mixinProp }}</div>
    </div>
</template>
<script>
    import Mixin from "./MixinWithProp"

    export default {
        mixins: [Mixin]
    }
</script>
""")
    myFixture.configureByText("ParentComp.vue", """
<template>
  <comp-with-mixin <caret>mixin-prop=123>1</comp-with-mixin>
</template>
<script>
  import CompWithMixin from './CompWithMixin'
  export default {
    components: { CompWithMixin }
  }
</script>
""")

    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertNotNull(property)
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertEquals("mixinProp", (property as JSProperty).name)
    TestCase.assertTrue(property.parent.parent is JSProperty)
    TestCase.assertEquals("props", (property.parent.parent as JSProperty).name)
    TestCase.assertEquals("MixinWithProp.vue", property.containingFile.name)
  }


  fun testTwoExternalMixins() {
    myFixture.configureByText("FirstMixin.vue", """
<script>
  export default {
    props: ['FirstMixinProp']
  }
</script>
""")
    myFixture.configureByText("SecondMixin.vue", """
<script>
  export default {
    props: ['SecondMixinProp']
  }
</script>
""")
    myFixture.configureByText("CompWithTwoMixins.vue", """
<template>
  <comp-with-two-mixins <caret>first-mixin-prop=1 second-mixin-prop=2 />
</template>
<script>
  import FirstMixin from './FirstMixin';
  import SecondMixin from './SecondMixin';

  export default {
    name: 'CompWithTwoMixins',
    mixins: [FirstMixin, SecondMixin]
  }
</script>
""")
    JSTestUtils.testES6<Exception>(project, {
      myFixture.checkHighlighting(true, false, true)

      val checkResolve = { propName: String, file: String ->
        val reference = myFixture.getReferenceAtCaretPosition()
        TestCase.assertNotNull(reference)
        val literal = reference!!.resolve()
        TestCase.assertNotNull(literal)
        TestCase.assertTrue(literal is JSLiteralExpression)
        TestCase.assertEquals(propName, (literal as JSLiteralExpression).stringValue)
        TestCase.assertTrue(literal.parent.parent is JSProperty)
        TestCase.assertEquals("props", (literal.parent.parent as JSProperty).name)
        TestCase.assertEquals(file, literal.containingFile.name)
      }
      checkResolve("FirstMixinProp", "FirstMixin.vue")

      val attribute = myFixture.findElementByText("second-mixin-prop", XmlAttribute::class.java)
      TestCase.assertNotNull(attribute)
      myFixture.editor.caretModel.moveToOffset(attribute.textOffset)
      checkResolve("SecondMixinProp", "SecondMixin.vue")
    })
  }

  fun testResolveIntoLocalMixin() {
    myFixture.configureByText("ResolveIntoLocalMixin.vue", """
<template>
    <local-mixin <caret>local-mixin-prop="1" local-prop="1"></local-mixin>
</template>

<script>
    let LocalMixin = {
        props: {
            localMixinProp: {
                required: true
            }
        }
    };

    export default {
        name: "local-mixin",
        mixins: [LocalMixin],
        props: {
            localProp: {}
        }
    }
</script>
""")

    doTestResolveIntoProperty("localMixinProp")
  }

  fun testResolveInMixinLiteral() {
    myFixture.configureByText("ResolveInMixinLiteral.vue", """
<template>
    <local-mixin <caret>prop-in-mixin-literal="11" local-prop="1"></local-mixin>
</template>

<script>
    export default {
        name: "local-mixin",
        mixins: [{
            props: {
                propInMixinLiteral: {}
            }
        }],
        props: {
            localProp: {}
        }
    }
</script>
""")

    doTestResolveIntoProperty("propInMixinLiteral")
  }

  private fun doTestResolveIntoProperty(name: String) {
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertNotNull(property)
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertEquals(name, (property as JSProperty).name)
    TestCase.assertTrue(property.parent.parent is JSProperty)
    TestCase.assertEquals("props", (property.parent.parent as JSProperty).name)
  }

  fun testResolveIntoGlobalMixin1() {
    myFixture.configureByText("GlobalMixins.js", globalMixinText())
    myFixture.configureByText("ResolveIntoGlobalMixin1.vue", """
<template>
    <local-comp <caret>hi2dden="found" interesting-prop="777"</local-comp>
</template>

<script>
    export default {
        name: "local-comp"
    }
</script>
""")
    doTestResolveIntoProperty("hi2dden")
  }

  fun testResolveIntoGlobalMixin2() {
    myFixture.configureByText("GlobalMixins.js", globalMixinText())
    myFixture.configureByText("ResolveIntoGlobalMixin2.vue", """
<template>
    <local-comp hi2dden="found" <caret>interesting-prop="777"</local-comp>
</template>

<script>
    export default {
        name: "local-comp"
    }
</script>
""")
    doTestResolveIntoProperty("interestingProp")
  }

  fun testTypeScriptResolve() {
    myFixture.configureByText("TypeScriptResolve.vue", """
<script lang="ts"><caret>encodeURI('a')</script>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val function = reference!!.resolve()
    TestCase.assertNotNull(function)
    TestCase.assertTrue(function is TypeScriptFunction)
    TestCase.assertEquals("lib.d.ts", function!!.containingFile.name)
  }

  fun testECMA5Resolve() {
    myFixture.configureByText("TypeScriptResolve.vue", """
<script><caret>encodeURI('a')</script>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val function = reference!!.resolve()
    TestCase.assertNotNull(function)
    TestCase.assertTrue(function is TypeScriptFunction)
    TestCase.assertEquals("lib.es5.d.ts", function!!.containingFile.name)
  }

  fun testVBindResolve() {
    myFixture.configureByText("VBindCommonResolve.vue", """
<template>
    <for-v-bind :<caret>test-prop.camel="1"></for-v-bind>
</template>
<script>
    export default {
        name: "for-v-bind",
        props: {
            testProp: {}
        }
    }
</script>
""")
    doTestResolveIntoProperty("testProp")
  }

  fun testResolveGlobalCustomDirective() {
    directivesTestCase(myFixture)
    val attribute = myFixture.findElementByText("v-focus", XmlAttribute::class.java)
    TestCase.assertNotNull(attribute)
    myFixture.editor.caretModel.moveToOffset(attribute.textOffset)
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val callExpression = reference!!.resolve()
    TestCase.assertNotNull(callExpression)
    // unstub for test
    TestCase.assertNotNull(callExpression!!.text)
    TestCase.assertEquals("focus", ((callExpression as JSCallExpression).arguments[0] as JSLiteralExpression).stringValue)
    TestCase.assertEquals("CustomDirectives.js", callExpression.containingFile.name)
  }

  fun testResolveLocalCustomDirective() {
    directivesTestCase(myFixture)
    val names = mapOf(Pair("v-local-directive", "localDirective"),
                      Pair("v-some-other-directive", "someOtherDirective"),
                      Pair("v-click-outside", "click-outside"),
                      Pair("v-imported-directive", "importedDirective"))
    names.forEach {
      val attribute = myFixture.findElementByText(it.key, XmlAttribute::class.java)
      TestCase.assertNotNull(attribute)
      myFixture.editor.caretModel.moveToOffset(attribute.textOffset)
      doTestResolveIntoDirective(it.value, if (it.value == "click-outside") "CustomDirectives.js" else "CustomDirectives.vue")
    }
  }

  private fun doTestResolveIntoDirective(directive: String, fileName : String) {
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertNotNull(directive, property)
    if (property is JSProperty) {
      TestCase.assertEquals(directive, property.name)
      TestCase.assertEquals(fileName, property.containingFile.name)
    } else if (property is JSCallExpression) {
      TestCase.assertNotNull(property.text)
      TestCase.assertEquals(directive, (property.arguments[0] as JSLiteralExpression).stringValue)
      TestCase.assertEquals(fileName, property.containingFile.name)
    } else {
      TestCase.assertTrue(false)
    }
  }

  fun testResolveIntoVueDefinitions() {
    createPackageJsonWithVueDependency(myFixture, "")
    myFixture.copyDirectoryToProject("../types/node_modules", "./node_modules")
    myFixture.configureByText("ResolveIntoVueDefinitions.vue", """
<script>
  export default {
    <caret>mixins: []
  }
</script>
""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val target = reference!!.resolve()
    TestCase.assertNotNull(target)
    TestCase.assertEquals("options.d.ts", target!!.containingFile.name)
    TestCase.assertTrue(target.parent is TypeScriptPropertySignature)
  }

  fun testResolveElementUiComponent() {
    createPackageJsonWithVueDependency(myFixture, "\"element-ui\": \"2.0.5\"")
    myFixture.copyDirectoryToProject("../libs/element-ui/node_modules", "./node_modules")
    val testData = arrayOf(
      Trinity("el-col", "ElCol", "col.js"),
      Trinity("el-button", "ElButton", "button.vue"),
      Trinity("el-button-group", "ElButtonGroup", "button-group.vue")
    )
    testData.forEach {
      myFixture.configureByText("ResolveElementUiComponent.vue", "<template><<caret>${it.first}></${it.first}></template>")
      doResolveIntoLibraryComponent(it.second, it.third)
    }
  }

  fun testResolveMintUiComponent() {
    createPackageJsonWithVueDependency(myFixture, "\"mint-ui\": \"^2.2.3\"")
    myFixture.copyDirectoryToProject("../libs/mint-ui/node_modules", "./node_modules")
    val testData = arrayOf(
      Trinity("mt-field", "mt-field", "field.vue"),
      Trinity("mt-swipe", "mt-swipe", "swipe.vue"),
      Trinity("mt-swipe-item", "mt-swipe-item", "swipe-item.vue")
    )
    testData.forEach {
      myFixture.configureByText("ResolveMintUiComponent.vue", "<template><<caret>${it.first}></${it.first}></template>")
      doResolveIntoLibraryComponent(it.second, it.third)
    }
  }

  fun testResolveVuetifyComponent() {
    createPackageJsonWithVueDependency(myFixture, "\"vuetify\": \"0.17.2\"")
    myFixture.copyDirectoryToProject("../libs/vuetify/node_modules", "./node_modules")
    val testData = arrayOf(
      Trinity("v-list", "v-list", "VList.js"),
      Trinity("v-list-tile-content", "v-list-tile-content", "index.js"),
      Trinity("v-app", "v-app", "VApp.js")
    )
    testData.forEach {
      myFixture.configureByText("ResolveVuetifyComponent.vue", "<template><<caret>${it.first}></${it.first}></template>")
      if (it.first == "v-list-tile-content") {
        val reference = myFixture.getReferenceAtCaretPosition()
        TestCase.assertNotNull(reference)
        val target = reference!!.resolve()
        TestCase.assertNotNull(target)
        TestCase.assertEquals(it.third, target!!.containingFile.name)
        TestCase.assertTrue(target.parent is JSCallExpression)
      } else {
        doResolveIntoLibraryComponent(it.second, it.third)
      }
    }
  }

  fun testElementUiDatePickerLikeComponent() {
    myFixture.configureByText("date-picker.js", """
export default {
    name: 'ElDatePicker',
    mixins: []
}
""")
    myFixture.configureByText("index.js", """
import DatePicker from './date-picker';

/* istanbul ignore next */
DatePicker.install = function install(Vue) {
  Vue.component(DatePicker.name, DatePicker);
};

export default DatePicker;
""")
    myFixture.configureByText("usage.vue", """
<template>
<<caret>el-date-picker />
</template>
""")
    doResolveIntoLibraryComponent("ElDatePicker", "date-picker.js")
  }

  fun testResolveSimpleObjectMemberComponent() {
    myFixture.configureByText("a.vue", "")
    myFixture.configureByText("lib-comp.es6",
"""
export default {
  name: 'lib-comp',
  template: '',
  render() {}
}
""")
    myFixture.configureByText("lib.es6",
"""
import LibComp from './lib-comp';
const obj = { LibComp };

Object.keys(obj).forEach(key => {
        Vue.component(key, obj[key]);
    });
""")
    myFixture.configureByText("ResolveSimpleObjectMemberComponent.vue",
"""<template><<caret>lib-comp/></template>""")
    doResolveIntoLibraryComponent("lib-comp", "lib-comp.es6")
  }

  fun testResolveAliasedObjectMemberComponent() {
    myFixture.configureByText("a.vue", "")
    myFixture.configureByText("lib-comp-for-alias.es6",
"""
export default {
  name: 'lib-comp',
  template: '',
  render() {}
}
""")
    myFixture.configureByText("libAlias.es6",
"""
import Alias from './lib-comp-for-alias';
const obj = { Alias };

Object.keys(obj).forEach(key => {
        Vue.component(key, obj[key]);
    });
""")
    myFixture.configureByText("ResolveAliasedObjectMemberComponent.vue",
"""<template><<caret>alias/></template>""")

    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val target = reference!!.resolve()
    TestCase.assertNotNull(target)
    TestCase.assertEquals("lib-comp-for-alias.es6", target!!.containingFile.name)
    TestCase.assertTrue(target.parent is JSObjectLiteralExpression)
  }

  fun testResolveObjectWithSpreadComponent() {
    myFixture.configureByText("a.vue", "")
    myFixture.configureByText("lib-spread.es6",
                              """
export default {
  name: 'lib-spread',
  template: '',
  render() {}
}
""")
    myFixture.configureByText("lib-register-spread.es6",
                              """
import LibSpread from './lib-spread';
const obj = { LibSpread };
const other = {...obj};

Object.keys(other).forEach(key => {
        Vue.component(key, other[key]);
    });
""")
    myFixture.configureByText("ResolveObjectWithSpreadComponent.vue",
                              """<template><<caret>lib-spread/></template>""")
    doResolveIntoLibraryComponent("lib-spread", "lib-spread.es6")
  }

  fun testResolveObjectWithSpreadComponentAliased() {
    myFixture.configureByText("a.vue", "")
    myFixture.configureByText("lib-spread.es6",
                              """
export default {
  name: 'lib-spread',
  template: '',
  render() {}
}
""")
    myFixture.configureByText("lib-register-spread.es6",
                              """
import LibSpreadAlias from './lib-spread';
const obj = { LibSpreadAlias };
const other = {...obj};

Object.keys(other).forEach(key => {
        Vue.component(key, other[key]);
    });
""")
    myFixture.configureByText("ResolveObjectWithSpreadComponentAliased.vue",
                              """<template><<caret>lib-spread-alias/></template>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val target = reference!!.resolve()
    TestCase.assertNotNull(target)
    TestCase.assertEquals("lib-spread.es6", target!!.containingFile.name)
    TestCase.assertTrue(target.parent is JSObjectLiteralExpression)
  }

  fun testResolveObjectWithSpreadLiteralComponent() {
    myFixture.configureByText("a.vue", "")
    myFixture.configureByText("lib-spread.es6",
                              """
export default {
  name: 'lib-spread',
  template: '',
  render() {}
}
""")
    myFixture.configureByText("lib-register-spread.es6",
                              """
import LibSpread from './lib-spread';
const other = {...{ LibSpread }};

Object.keys(other).forEach(key => {
        Vue.component(key, other[key]);
    });
""")
    myFixture.configureByText("ResolveObjectWithSpreadLiteralComponent.vue",
                              """<template><<caret>lib-spread/></template>""")
    doResolveIntoLibraryComponent("lib-spread", "lib-spread.es6")
  }

  fun testResolveWithExplicitForInComponentsBindingEs6() {
    JSTestUtils.testES6<Exception>(myFixture.project, {
      myFixture.configureByText("a.vue", "")
      myFixture.configureByText("CompForForIn.es6",
                                """export default {
name: 'compForForIn',
template: '',
render() {}""")
      myFixture.configureByText("register.es6", """
import CompForForIn from './CompForForIn';

      const components = {
        CompForForIn
      }

components.install = (Vue, options = {}) => {
    for (const componentName in components) {
        const component = components[componentName]

        if (component && componentName !== 'install') {
            Vue.component(component.name, component)
        }
    }
}
""")
      myFixture.configureByText("ResolveWithExplicitForInComponentsBinding.vue",
                                """<template><<caret>CompForForIn/></template>""")
      doResolveIntoLibraryComponent("compForForIn", "CompForForIn.es6")
    })
  }

  fun testResolveWithExplicitForInComponentsBinding() {
    JSTestUtils.testES6<Exception>(myFixture.project, {
      myFixture.configureByText("a.vue", "")
      myFixture.configureByText("CompForForIn.vue",
                                """<script>export default {
name: 'compForForIn',
template: '',
render() {}</script>""")
      myFixture.configureByText("register.es6", """
import CompForForIn from './CompForForIn';

      const components = {
        CompForForIn
      }

components.install = (Vue, options = {}) => {
    for (const componentName in components) {
        const component = components[componentName]

        if (component && componentName !== 'install') {
            Vue.component(component.name, component)
        }
    }
}
""")
      myFixture.configureByText("ResolveWithExplicitForInComponentsBinding.vue",
                                """<template><<caret>CompForForIn/></template>""")
      doResolveIntoLibraryComponent("compForForIn", "CompForForIn.vue")
    })
  }

  fun testResolveWithClassComponent() {
    JSTestUtils.testES6<Exception>(myFixture.project, {
      createTwoClassComponents(myFixture)
      myFixture.configureByText("ResolveWithClassComponent.vue",
                                """
<template>
  <<caret>ShortVue/>
  <LongComponent/>
</template>
<script>
import { Component, Vue } from 'vue-property-decorator';
import ShortComponent from './ShortComponent';
import LongComponent from './LongComponent';

@Component({
  components: {
    shortVue: ShortComponent,
    LongComponent
  }
})
export default class UsageComponent extends Vue {
}
</script>
""")
      doResolveIntoClassComponent("ShortComponent.vue")
    })
  }

  fun testResolveWithClassComponentTs() {
    JSTestUtils.testES6<Exception>(myFixture.project, {
      createTwoClassComponents(myFixture, true)
      myFixture.configureByText("ResolveWithClassComponentTs.vue",
                                """
<template>
  <ShortVue/>
  <<caret>LongComponent/>
</template>
<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import ShortComponent from './ShortComponent';
import LongComponent from './LongComponent';

@Component({
  components: {
    shortVue: ShortComponent,
    LongComponent
  }
})
export default class UsageComponent extends Vue {
}
</script>
""")
      doResolveIntoLibraryComponent("long-vue", "LongComponent.vue")
    })
  }

  fun testLocalComponentsExtendsResolve() {
    JSTestUtils.testES6<Exception>(myFixture.project, {
      createLocalComponentsExtendsData(myFixture, false)
      myFixture.type("prop-from-a=\"\"")
      myFixture.editor.caretModel.moveToOffset(myFixture.editor.caretModel.offset - 5)
      doTestResolveIntoProperty("propFromA")
    })
  }

  private fun doResolveIntoClassComponent(fileName: String, checkType: Boolean = true) {
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val target = reference!!.resolve()
    TestCase.assertNotNull(target)
    TestCase.assertEquals(fileName, target!!.containingFile.name)
    if (checkType) {
      TestCase.assertTrue(target.parent is JSClassExpression<*>)
    }
  }

  private fun doResolveIntoLibraryComponent(compName: String, fileName: String) {
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val target = reference!!.resolve()
    TestCase.assertNotNull(target)
    TestCase.assertEquals(fileName, target!!.containingFile.name)
    TestCase.assertTrue(target.parent is JSProperty)
    TestCase.assertEquals(compName, StringUtil.unquoteString((target.parent as JSProperty).value!!.text))
  }

  fun testResolveWithRecursiveMixins() {
    JSTestUtils.testES6<Exception>(myFixture.project, {
      defineRecursiveMixedMixins(myFixture)
      myFixture.configureByText("ResolveWithRecursiveMixins.vue", """
        <template>
          <<caret>HiddenComponent/>
        </template>
      """)
      doResolveIntoLibraryComponent("hidden-component", "hidden-component.vue")
      myFixture.configureByText("ResolveWithRecursiveMixins2.vue", """
        <template>
          <<caret>OneMoreComponent/>
        </template>
      """)
      doResolveIntoClassComponent("OneMoreComponent.vue", false)
    })
  }

  fun testCssClassInPug() {
    myFixture.configureByText("foo.vue", "<template lang='pug'>\n" +
                                         "    .someClass\n" +
                                         "</template>\n" +
                                         "<style>\n" +
                                         "    .someClass<caret> {}\n" +
                                         "</style>")
    val usages = myFixture.findUsages(myFixture.elementAtCaret)
    assertEquals(2, usages.size)
  }
}

fun globalMixinText(): String {
  return """
  let mixin = {
      props: {
          hi2dden: {}
      }
  };

  Vue.mixin(mixin);

  Vue.mixin({
      props: {
          interestingProp: {},
          requiredMixinProp: {
            required: true
          }
      }
  });
  """
}