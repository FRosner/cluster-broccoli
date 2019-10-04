package de.frosner.broccoli.services

import javax.inject.{Inject, Singleton}
import de.frosner.broccoli.models.Template
import de.frosner.broccoli.templates._

@Singleton
class TemplateService @Inject()(templateSource: TemplateSource) {
  def getTemplates: Seq[Template] = getTemplates(false)

  def getTemplates(refreshed: Boolean): Seq[Template] = templateSource.loadTemplates(refreshed)

  def template(id: String): Option[Template] = getTemplates.find(_.id == id)

}
