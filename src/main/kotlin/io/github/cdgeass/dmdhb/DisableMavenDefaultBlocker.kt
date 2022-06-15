package io.github.cdgeass.dmdhb

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * @author cdgeass
 * @since  2022-06-08
 */
class DisableMavenDefaultBlocker : StartupActivity.DumbAware {

    private val LOG = logger<DisableMavenDefaultBlocker>()

    override fun runActivity(project: Project) {
        val pluginsPath = PathManager.getPreInstalledPluginsPath()

        LOG.debug("PreInstalled plugins path $pluginsPath")

        loadSettings("$pluginsPath/maven/lib/maven3/conf/settings.xml")
    }

    private fun loadSettings(path: String) {
        val dbf = DocumentBuilderFactory.newDefaultInstance()
        try {
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(path)

            val mirrorsNodes = doc.getElementsByTagName("mirrors")
            if (mirrorsNodes.length == 1) {
                val mirrors = mirrorsNodes.item(0) ?: return
                val mirrorNodes = mirrors.childNodes
                if (mirrorNodes.length >= 1) {
                    for (i in 0 until mirrorNodes.length) {
                        val mirror = mirrorNodes.item(i)
                        val childNodes = mirror.childNodes
                        if (childNodes.length >= 1) {
                            for (j in 0 until childNodes.length) {
                                val childNode = childNodes.item(j)
                                if (childNode.nodeName == "id") {
                                    val data = childNode.firstChild
                                    if (data is org.w3c.dom.CharacterData) {
                                        if (data.data == "maven-default-http-blocker") {
                                            mirrors.removeChild(mirror)

                                            val tf = TransformerFactory.newInstance()
                                            val transformer = tf.newTransformer()

                                            val source = DOMSource(doc)
                                            val result = StreamResult(File(path))

                                            transformer.transform(source, result)
                                            LOG.info("Success delete the maven-default-http-blocker!")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.error("Delete maven-default-http-blocker error!", e)
        }
    }
}