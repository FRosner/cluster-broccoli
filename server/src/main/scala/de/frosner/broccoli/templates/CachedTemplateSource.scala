package de.frosner.broccoli.templates

import de.frosner.broccoli.models.Template

/**
  * The template source that wraps another template source and caches loaded templates of that source
  *
  * @param source The template source that will be cached
  */
class CachedTemplateSource(source: TemplateSource) extends TemplateSource {
  @volatile private var templatesCache: Option[Seq[Template]] = None

  override val templateRenderer: TemplateRenderer = source.templateRenderer

  override def loadTemplates(refreshed: Boolean): Seq[Template] = {
    if (refreshed) {
      templatesCache = None
    }
    templatesCache match {
      case Some(templates) => templates
      case None =>
        refresh()
        templatesCache.get
    }
  }

  def refresh(): Unit = templatesCache = Some(source.loadTemplates)
}
