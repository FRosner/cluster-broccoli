module Templates.Models exposing (..)

import Dict exposing (..)

type alias TemplateId = String

type alias Template =
  { id : TemplateId
  , description : String
  , version : String
  }

type alias NewInstanceForm =
  { selectedTemplate : Template
  }

type alias TemplateWithForms =
  { template : Template
  , newInstanceForm : Maybe NewInstanceForm
  }

type alias Model = Dict TemplateId TemplateWithForms

initialModel : Model
initialModel = Dict.empty
