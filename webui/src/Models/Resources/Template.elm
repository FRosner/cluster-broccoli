module Models.Resources.Template exposing (..)

import Json.Decode as Decode exposing (field)
import Json.Encode as Encode
import Dict exposing (Dict)
import Utils.DictUtils exposing (flatten)
import Maybe.Extra exposing (isJust)
import Utils.StringUtils exposing (surround)
import Maybe.Extra exposing (join)


type alias TemplateId =
    String


type alias Template =
    { id : TemplateId
    , description : String
    , version : String
    , parameters : List String
    , parameterInfos : Dict String ParameterInfo
    }


type ParameterType
    = RawParam
    | StringParam
    | IntParam
    | DecimalParam


type ParameterValue
    = IntVal Int
    | StringVal String
    | RawVal String
    | DecimalVal Float -- Looks like Elm does not support bigger decimals like Java's BigDecimal


type alias ParameterInfo =
    { id : String
    , name : Maybe String
    , default : Maybe ParameterValue
    , secret : Maybe Bool
    , orderIndex : Maybe Float
    , dataType : ParameterType
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
    (field "type" decodeDataType)
        |> Decode.andThen
            (\paramType ->
                Decode.map6 ParameterInfo
                    (field "id" Decode.string)
                    (Decode.maybe (field "name" Decode.string))
                    (Decode.maybe (field "default" (decodeParamValue paramType)))
                    (Decode.maybe (field "secret" Decode.bool))
                    (Decode.maybe (field "orderIndex" Decode.float))
                    (Decode.succeed (paramType))
            )


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


decodeValueFromInfo : Dict String ParameterInfo -> Decode.Decoder (Dict String ParameterValue)
decodeValueFromInfo parameterInfos =
    decodeMaybeValueFromInfo parameterInfos
        |> Decode.andThen
            (\paramValues ->
                Decode.succeed (flatten paramValues)
            )


decodeMaybeValueFromInfo : Dict String ParameterInfo -> Decode.Decoder (Dict String (Maybe ParameterValue))
decodeMaybeValueFromInfo parameterInfos =
    (Decode.dict Decode.value)
        |> Decode.andThen
            (\paramValuesRaw ->
                Decode.succeed
                    (Dict.map
                        (\paramName paramValueRaw ->
                            join
                                (Dict.get paramName parameterInfos
                                    |> Maybe.map
                                        (\paramInfo -> paramInfo.dataType)
                                    |> Maybe.andThen
                                        (\paramType ->
                                            Result.toMaybe (Decode.decodeValue (Decode.nullable (decodeParamValue paramType)) paramValueRaw)
                                        )
                                )
                        )
                        paramValuesRaw
                    )
            )


decodeParamValue : ParameterType -> Decode.Decoder ParameterValue
decodeParamValue paramType =
    case paramType of
        IntParam ->
            Decode.int
                |> Decode.andThen
                    (\paramValue ->
                        Decode.succeed (IntVal paramValue)
                    )

        DecimalParam ->
            Decode.float
                |> Decode.andThen
                    (\paramValue ->
                        Decode.succeed (DecimalVal paramValue)
                    )

        StringParam ->
            Decode.string
                |> Decode.andThen
                    (\paramValue ->
                        Decode.succeed (StringVal paramValue)
                    )

        RawParam ->
            Decode.string
                |> Decode.andThen
                    (\paramValue ->
                        Decode.succeed (RawVal paramValue)
                    )


encodeParamValue paramValue =
    case paramValue of
        IntVal x ->
            Encode.int x

        DecimalVal x ->
            Encode.float x

        StringVal x ->
            Encode.string x

        RawVal x ->
            Encode.string x


valueFromStringAndInfo : Dict String ParameterInfo -> String -> String -> Result String ParameterValue
valueFromStringAndInfo parameterInfos paramName paramValue =
    let
        maybeParamType =
            Dict.get paramName parameterInfos
                |> Maybe.map (\info -> info.dataType)
    in
        -- This should never happen
        Result.fromMaybe "Internal Error. Contact Admin." maybeParamType
            |> Result.andThen (\paramType -> valueFromString paramType paramValue)


valueFromString : ParameterType -> String -> Result String ParameterValue
valueFromString paramType paramValue =
    case (Decode.decodeString (decodeParamValue paramType) (wrapValue paramType paramValue)) of
        Ok res ->
            Ok res

        Err msg ->
            Err ("Not a valid " ++ (dataTypeToString paramType))


maybeValueToString : Maybe ParameterValue -> Maybe String
maybeValueToString maybeParamValue =
    maybeParamValue
        |> Maybe.andThen (\paramValue -> Just (valueToString paramValue))


valueToString : ParameterValue -> String
valueToString paramValue =
    case paramValue of
        StringVal val ->
            val

        IntVal val ->
            toString val

        DecimalVal val ->
            toString val

        RawVal val ->
            val


dataTypeToString dataType =
    case dataType of
        -- we do not validate string and raw fields
        StringParam ->
            "string"

        RawParam ->
            "string"

        IntParam ->
            "integer"

        DecimalParam ->
            "decimal"


wrapValue paramType paramValue =
    case paramType of
        StringParam ->
            surround "\"" paramValue

        RawParam ->
            surround "\"" paramValue

        _ ->
            paramValue
