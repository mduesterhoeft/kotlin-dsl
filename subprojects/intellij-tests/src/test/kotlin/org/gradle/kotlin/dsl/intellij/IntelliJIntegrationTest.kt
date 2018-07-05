/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.gradle.kotlin.dsl.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile

import com.intellij.testFramework.EdtTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory

import com.intellij.util.ThrowableRunnable

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat

import org.jetbrains.kotlin.idea.core.script.IdeScriptReportSink
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager.Companion.updateScriptDependenciesSynchronously
import org.jetbrains.kotlin.idea.core.script.isScriptDependenciesUpdaterDisabled
import org.jetbrains.kotlin.script.KotlinScriptDefinition

import org.junit.Test

import kotlin.reflect.KMutableProperty0


class IntelliJIntegrationTest : GradleImportingTestCase() {

    private
    lateinit var codeInsightTestFixture: CodeInsightTestFixture

    override fun setUpFixtures() {
        val fixtureFactory = IdeaTestFixtureFactory.getFixtureFactory()
        myTestFixture = fixtureFactory.createFixtureBuilder(getName()).fixture
        codeInsightTestFixture = fixtureFactory.createCodeInsightFixture(myTestFixture)
        codeInsightTestFixture.setUp()
        ApplicationManager.getApplication().isScriptDependenciesUpdaterDisabled = true
    }

    override fun tearDownFixtures() {
        ApplicationManager.getApplication().isScriptDependenciesUpdaterDisabled = false
        codeInsightTestFixture.tearDown()
        (this::codeInsightTestFixture as KMutableProperty0<CodeInsightTestFixture?>).set(null)
        myTestFixture = null
    }

    @Test
    fun testScriptTemplates() {

        val settingsFile =
            createProjectSubFile("settings.gradle.kts", "")

        val buildFile =
            createProjectSubFile("build.gradle.kts", "")

        importProject()

        runInEdtAndWait {

            assertThat(
                kotlinScriptDefinitionFor(settingsFile)?.template?.qualifiedName,
                equalTo("org.gradle.kotlin.dsl.KotlinSettingsScript"))

            assertThat(
                kotlinScriptDefinitionFor(buildFile)?.template?.qualifiedName,
                equalTo("org.gradle.kotlin.dsl.KotlinBuildScript"))
        }
    }

    @Test
    fun testBuildFileHighlighting() {

        val settingsFile =
            createProjectSubFile("settings.gradle.kts", "")

        val buildFile =
            createProjectSubFile(
                "build.gradle.kts",
                """

                    plugins {
                        kotlin("jvm") version "1.2.41"
                    }

                    dependencies {
                        compile(kotlin("stdlib"))
                    }

                """.trimIndent()
            )

        importProject()

        runInEdtAndWait {
            codeInsightTestFixture.openFileInEditor(buildFile)
            updateScriptDependenciesSynchronously(buildFile, codeInsightTestFixture.project)
        }

        val reports = buildFile.getUserData(IdeScriptReportSink.Reports)!!
        println("IdeScriptReportSink.Reports: $reports")

        runInEdtAndWait {
            codeInsightTestFixture.checkHighlighting()
        }
    }

    private
    fun kotlinScriptDefinitionFor(buildFile: VirtualFile): KotlinScriptDefinition? =
        ScriptDefinitionsManager.getInstance(myProject).findScriptDefinition(buildFile)

    private
    fun runInEdtAndWait(runnable: () -> Unit) {
        EdtTestUtil.runInEdtAndWait(ThrowableRunnable { runnable() })
//        ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.NON_MODAL)
    }
}
