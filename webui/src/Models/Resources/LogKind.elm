module Models.Resources.LogKind exposing (..)

{-| Provides log kind
-}


{-| The kind of log to view
-}
type LogKind
    = StdOut
    | StdErr


toParameter : LogKind -> String
toParameter kind =
    case kind of
        StdOut ->
            "stdout"

        StdErr ->
            "stderr"
