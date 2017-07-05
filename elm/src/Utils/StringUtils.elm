module Utils.StringUtils exposing (..)

import Http


errorToString : String -> Http.Error -> String
errorToString prefix error =
    case error of
        Http.BadStatus request ->
            String.concat
                [ prefix
                , ": "
                , (toString request.status.code)
                , " ("
                , request.status.message
                , ")"
                ]

        _ ->
            (toString error)
