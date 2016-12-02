module Models.Resources.Template exposing (..)

type alias TemplateId = String

type alias Template =
  { id : TemplateId
  , description : String
  , version : String
  , parameters : List String
  }

addTemplateInstanceString template =
    String.concat ["New ", template.id, " instance"]
