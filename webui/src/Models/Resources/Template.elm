module Models.Resources.Template exposing (..)

import Json.Decode as Decode exposing (field)
import Json.Encode as Encode
import Dict exposing (Dict)
import Maybe.Extra exposing (isJust)
import Utils.StringUtils exposing (surround)


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
    , default : Maybe ParameterValue
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
    Decode.maybe (field "type" decodeDataType)
        |> Decode.andThen
            (\maybeParamType ->
                let
                    paramType =
                        maybeParamType |> Maybe.withDefault RawParam
                in
                    Decode.map6 ParameterInfo
                        (field "id" Decode.string)
                        (Decode.maybe (field "name" Decode.string))
                        (Decode.maybe (field "default" (decodeParamValue paramType)))
                        (Decode.maybe (field "secret" Decode.bool))
                        (Decode.maybe (field "orderIndex" Decode.float))
                        {- We always succeed because even if the decode phase failed
                           - we have wrapped it in a Maybe
                        -}
                        (Decode.succeed (maybeParamType))
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
                -- Using foldl as a flatMap since Elm does not have flatMap
                Decode.succeed (Dict.foldl (\k v acc -> Dict.update k (always v) acc) Dict.empty paramValues)
            )


decodeMaybeValueFromInfo : Dict String ParameterInfo -> Decode.Decoder (Dict String (Maybe ParameterValue))
decodeMaybeValueFromInfo parameterInfos =
    (Decode.dict Decode.value)
        |> Decode.andThen
            (\paramValuesRaw ->
                Decode.succeed
                    (Dict.map
                        (\paramName paramValueRaw ->
                            let
                                paramType =
                                    Dict.get paramName parameterInfos
                                        |> Maybe.andThen
                                            (\paramInfo -> paramInfo.dataType)
                                        |> Maybe.withDefault RawParam
                            in
                                case (Decode.decodeValue (Decode.nullable (decodeParamValue paramType)) paramValueRaw) of
                                    Ok paramValue ->
                                        paramValue

                                    -- Note: This should never really happen since it is coming from the backend?
                                    Err error ->
                                        Nothing
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
                        Decode.succeed (IntParamVal paramValue)
                    )

        DecimalParam ->
            Decode.float
                |> Decode.andThen
                    (\paramValue ->
                        Decode.succeed (DecimalParamVal paramValue)
                    )

        StringParam ->
            Decode.string
                |> Decode.andThen
                    (\paramValue ->
                        Decode.succeed (StringParamVal paramValue)
                    )

        RawParam ->
            Decode.string
                |> Decode.andThen
                    (\paramValue ->
                        Decode.succeed (RawParamVal paramValue)
                    )


encodeParamValue paramValue =
    case paramValue of
        IntParamVal x ->
            Encode.int x

        DecimalParamVal x ->
            Encode.float x

        StringParamVal x ->
            Encode.string x

        RawParamVal x ->
            Encode.string x


valueFromStringAndInfo : Dict String ParameterInfo -> String -> String -> Result String ParameterValue
valueFromStringAndInfo parameterInfos paramName paramValue =
    let
        paramType =
            Dict.get paramName parameterInfos
                |> Maybe.andThen (\info -> info.dataType)
                |> Maybe.withDefault RawParam
    in
        valueFromString paramType paramValue


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
        StringParamVal val ->
            val

        IntParamVal val ->
            toString val

        DecimalParamVal val ->
            toString val

        RawParamVal val ->
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
