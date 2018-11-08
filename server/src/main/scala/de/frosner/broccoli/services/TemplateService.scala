package de.frosner.broccoli.services

import de.frosner.broccoli.auth.Account
import javax.inject.{Inject, Singleton}
import de.frosner.broccoli.models.Template
import de.frosner.broccoli.templates._

@Singleton
class TemplateService @Inject()(templateSource: TemplateSource) {
  def getTemplates(account: Account): Seq[Template] = templateSource.loadTemplates(account)

  def template(account: Account, id: String): Option[Template] = getTemplates(account).find(_.id == id)

}
