module Models.Resources.Template exposing (..)

import Json.Decode as Decode exposing (field)
import Dict exposing (Dict)


type alias TemplateId =
    String


type alias Template =
    { id : TemplateId
    , description : String
    , version : String
    , parameters : List String
    , parameterInfos : Dict String ParameterInfo
    }



-- We add Param at the end to avoid conflict with traditional data types


type ParameterType
    = RawParam
    | StringParam
    | IntParam
    | DecimalParam


type ParameterValue
    = IntParamVal Int
    | StringParamVal String
    | RawParamVal String
    | DecimalParamVal Float -- Looks like Elm does not support bigger decimals like Java's BigDecimal


type alias ParameterInfo =
    { id : String
    , name : Maybe String
    , default : Maybe String
    , secret : Maybe Bool
    , orderIndex : Maybe Float
    , dataType : Maybe ParameterType
    }


addTemplateInstanceString template =
    String.concat [ "New ", template.id, " instance" ]


decoder =
    Decode.map5 Template
        (field "id" Decode.string)
        (field "description" Decode.string)
        (field "version" Decode.string)
        (field "parameters" (Decode.list Decode.string))
        (field "parameterInfos" (Decode.dict parameterInfoDecoder))


parameterInfoDecoder =
    Decode.map6 ParameterInfo
        (field "id" Decode.string)
        (Decode.maybe (field "name" Decode.string))
        (Decode.maybe (field "default" Decode.string))
        (Decode.maybe (field "secret" Decode.bool))
        (Decode.maybe (field "orderIndex" Decode.float))
        (Decode.maybe (field "type" decodeDataType))


decodeDataType : Decode.Decoder ParameterType
decodeDataType =
    Decode.string
        |> Decode.andThen
            (\dataType ->
                case dataType of
                    "integer" ->
                        Decode.succeed IntParam

                    "decimal" ->
                        Decode.succeed DecimalParam

                    "string" ->
                        Decode.succeed StringParam

                    "raw" ->
                        Decode.succeed RawParam

                    _ ->
                        Decode.fail <| "Unknown dataType: " ++ dataType
            )
