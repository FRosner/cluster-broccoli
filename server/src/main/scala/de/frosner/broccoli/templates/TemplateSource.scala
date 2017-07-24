package de.frosner.broccoli.templates

import de.frosner.broccoli.models.Template

trait TemplateSource {
  def loadTemplates(): Seq[Template]
}
