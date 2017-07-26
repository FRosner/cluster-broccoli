package de.frosner.broccoli.templates

import de.frosner.broccoli.models.Template

/**
  * Provide a source of templates to create an instances from
  */
trait TemplateSource {

  /**
    * Get templates from the source.
    *
    * Does not guarantee to return the same templates on repeated invocations. Can potentially perform I/O
    */
  def loadTemplates(): Seq[Template]
}
