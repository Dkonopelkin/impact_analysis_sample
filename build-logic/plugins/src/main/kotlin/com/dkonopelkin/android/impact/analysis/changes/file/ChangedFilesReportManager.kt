package com.dkonopelkin.android.impact.analysis.changes.file

import org.gradle.api.Project
import org.jdom2.input.DOMBuilder
import org.w3c.dom.Document
import org.w3c.dom.Element
import com.dkonopelkin.android.impact.analysis.changes.model.ChangeType
import com.dkonopelkin.android.impact.analysis.changes.model.ChangedFile
import com.dkonopelkin.android.impact.analysis.changes.model.ModuleData
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Класс предназначен для чтения и записи информации о измененных файлах в XML файл
 * */
internal class ChangedFilesReportManager {

    fun readChangedFilesFromReport(
        reportFile: File,
    ): Map<ModuleData, List<ChangedFile>> {
        if (!reportFile.exists()) {
            return emptyMap()
        }
        val result = mutableMapOf<ModuleData, List<ChangedFile>>()

        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = DOMBuilder().build(builder.parse(reportFile))
        val rootTag = document.rootElement

        val moduleListTag = rootTag.getChildren(TAG_MODULE)
        moduleListTag.forEach { moduleElement ->
            val moduleData = ModuleData(
                name = moduleElement.getAttribute(TAG_MODULE_ATTR_NAME).value,
                relativePath = moduleElement.getAttribute(TAG_MODULE_ATTR_RELATIVE_PATH).value
            )
            val fileListTag = moduleElement.getChild(TAG_CHANGED_FILES).getChildren(TAG_FILE)
            val changedFileList = fileListTag.map { fileElement ->
                val rootDir = fileElement.getAttribute(TAG_FILE_ATTR_ROOT_DIR).value
                val path = fileElement.getAttribute(TAG_FILE_ATTR_PATH).value
                val changeType = fileElement.getAttribute(TAG_FILE_ATTR_CHANGE_TYPE).value
                ChangedFile(
                    rootDir = File(rootDir),
                    file = File(path),
                    changeType = ChangeType.getTypeByCode(changeType[0]),
                )
            }
            result[moduleData] = changedFileList
        }
        return result
    }

    @Suppress("PrintStackTrace")
    fun writeChangedFilesToReport(
        reportFile: File,
        changes: Map<ModuleData, List<ChangedFile>>
    ) {
        if (!reportFile.parentFile.exists()) {
            reportFile.parentFile.mkdirs()
        }
        try {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
            val rootTag = document.createElement(ROOT_TAG)
            document.appendChild(rootTag)

            changes.forEach { (moduleInfo, changes) ->
                val moduleChangesInfo = createModuleChangesInfoTag(
                    document = document,
                    moduleData = moduleInfo,
                    changes = changes
                )
                rootTag.appendChild(moduleChangesInfo)
            }

            val transformer = TransformerFactory.newInstance().newTransformer().apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
            }
            val source = DOMSource(document)
            val result = StreamResult(reportFile)
            transformer.transform(source, result)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createModuleChangesInfoTag(
        document: Document,
        moduleData: ModuleData,
        changes: List<ChangedFile>,
    ): Element {
        val moduleTag = document.createElement(TAG_MODULE)

        moduleTag.setAttributeNode(document.createAttribute(TAG_MODULE_ATTR_NAME).apply {
            value = moduleData.name
        })
        moduleTag.setAttributeNode(document.createAttribute(TAG_MODULE_ATTR_RELATIVE_PATH).apply {
            value = moduleData.relativePath
        })

        val changedFileListTag = document.createElement(TAG_CHANGED_FILES)
        moduleTag.appendChild(changedFileListTag)

        changes.forEach { changedFile ->
            val fileTag = createFileChangesTag(document, changedFile)
            changedFileListTag.appendChild(fileTag)
        }

        return moduleTag
    }

    private fun createFileChangesTag(
        document: Document,
        change: ChangedFile,
    ): Element {
        val fileElement = document.createElement(TAG_FILE)

        fileElement.setAttributeNode(document.createAttribute(TAG_FILE_ATTR_ROOT_DIR).apply {
            value = change.rootDir.path
        })
        fileElement.setAttributeNode(document.createAttribute(TAG_FILE_ATTR_PATH).apply {
            value = change.file.path
        })
        fileElement.setAttributeNode(document.createAttribute(TAG_FILE_ATTR_CHANGE_TYPE).apply {
            value = change.changeType.code.toString()
        })

        return fileElement
    }

    fun getDefaultReportFile(rootProject: Project): File {
        val projectPath = rootProject.projectDir.absolutePath
        val filePath = "$projectPath/impact/"
        val reportFile = File(filePath, "impactAnalysisChangedFilesResult.xml")
        return reportFile
    }

    companion object {

        private const val ROOT_TAG = "fileChanges"

        private const val TAG_MODULE = "module"
        private const val TAG_MODULE_ATTR_NAME = "name"
        private const val TAG_MODULE_ATTR_RELATIVE_PATH = "relativePath"

        private const val TAG_CHANGED_FILES = "changedFiles"

        private const val TAG_FILE = "file"
        private const val TAG_FILE_ATTR_ROOT_DIR = "rootDir"
        private const val TAG_FILE_ATTR_PATH = "path"
        private const val TAG_FILE_ATTR_CHANGE_TYPE = "changeType"
    }
}