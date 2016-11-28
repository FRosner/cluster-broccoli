module Templates.Models exposing (..)

import Dict exposing (..)

type alias TemplateId = String

type alias Template =
  { id : TemplateId
  , description : String
  , version : String
  , parameters : List String
  }

type alias ParameterValues = Dict String (Maybe String)

type alias NewInstanceForm =
  { selectedTemplate : Template
  , visible : Bool
  , parameterValues : ParameterValues
  }

setVisible : NewInstanceForm -> Bool -> NewInstanceForm
setVisible form visibility =
  { form | visible = visibility }

type alias TemplateWithForms =
  { template : Template
  , newInstanceForm : NewInstanceForm
  }

type alias Model = Dict TemplateId TemplateWithForms

initialModel : Model
initialModel = Dict.empty
