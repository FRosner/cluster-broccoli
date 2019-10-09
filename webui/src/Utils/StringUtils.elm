module Utils.StringUtils exposing (..)

import Http


errorToString : String -> Http.Error -> String
errorToString prefix error =
    case error of
        Http.BadStatus request ->
            String.concat
                [ prefix
                , ": "
                , toString request.status.code
                , " ("
                , request.status.message
                , ")"
                ]

        _ ->
            toString error


{-| Surround a string with another string.
surround "bar" "foo" == "barfoobar"
-}
surround : String -> String -> String
surround wrap string =
    wrap ++ string ++ wrap


{-| Remove surrounding strings from another string.
unsurround "foo" "foobarfoo" == "bar"
-}
unsurround : String -> String -> String
unsurround wrap string =
    if String.startsWith wrap string && String.endsWith wrap string then
        let
            length =
                String.length wrap
        in
        string
            |> String.dropLeft length
            |> String.dropRight length

    else
        string
