package de.frosner.broccoli.templates

import de.frosner.broccoli.models.Template

/**
  * Returns a sequence of templates loaded from a source
  */
trait TemplateSource {
  def loadTemplates(): Seq[Template]
}
