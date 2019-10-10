module Utils.DictUtils exposing (flatMap, flatten)

import Dict exposing (Dict)


flatMap : (comparable -> b -> Maybe c) -> Dict comparable b -> Dict comparable c
flatMap apply inDict =
    -- Using foldl as a flatMap since Elm does not have flatMap
    Dict.foldl
        (\k v acc ->
            Dict.update k (always (apply k v)) acc
        )
        Dict.empty
        inDict


flatten : Dict comparable (Maybe b) -> Dict comparable b
flatten inDict =
    let
        mapFunc k v =
            v
    in
    flatMap mapFunc inDict
