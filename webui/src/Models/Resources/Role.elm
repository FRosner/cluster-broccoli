module Models.Resources.Role exposing (Role(..), decoder, encoder)

import Json.Decode as Decode exposing (field)
import Json.Encode as Encode


type Role
    = Administrator
    | Operator
    | User


decoder : Decode.Decoder Role
decoder =
    let
        stringToRole s =
            case s of
                "administrator" ->
                    Administrator

                "operator" ->
                    Operator

                _ ->
                    User
    in
    Decode.andThen
        (\statusString -> Decode.succeed (stringToRole statusString))
        Decode.string


encoder : Role -> Encode.Value
encoder role =
    let
        roleToString s =
            case s of
                Administrator ->
                    "administrator"

                Operator ->
                    "operator"

                User ->
                    "user"
    in
    role
        |> roleToString
        |> Encode.string
