module Utils.MaybeUtils exposing (concat, isDefined)

import Maybe exposing (Maybe)


concat : Maybe (Maybe a) -> Maybe a
concat maybeMaybeA =
    case maybeMaybeA of
        Nothing ->
            Nothing

        Just Nothing ->
            Nothing

        Just (Just v) ->
            Just v


isDefined : Maybe a -> Bool
isDefined maybeA =
    case maybeA of
        Just _ ->
            True

        Nothing ->
            False
