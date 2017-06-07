module Models.Resources.Template exposing (..)

import Json.Decode as Decode exposing (field)
import Dict exposing (Dict)

type alias TemplateId = String

type alias Template =
  { id : TemplateId
  , description : String
  , version : String
  , parameters : List String
  , parameterInfos : Dict String ParameterInfo
  }

type alias ParameterInfo =
  { id : String
  , name : Maybe String
  , default : Maybe String
  , secret : Maybe Bool
  }

addTemplateInstanceString template =
    String.concat ["New ", template.id, " instance"]

decoder =
  Decode.map5 Template
    (field "id" Decode.string)
    (field "description" Decode.string)
    (field "version" Decode.string)
    (field "parameters" (Decode.list Decode.string))
    (field "parameterInfos" (Decode.dict parameterInfoDecoder))

parameterInfoDecoder =
  Decode.map4 ParameterInfo
    (field "id" Decode.string)
    (Decode.maybe (field "name" Decode.string))
    (Decode.maybe (field "default" Decode.string))
    (Decode.maybe (field "secret" Decode.bool))
