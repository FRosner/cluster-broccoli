module Utils.MaybeUtils exposing (concat, concatMap, isDefined)

import Maybe exposing (Maybe)

concat : Maybe (Maybe a) -> Maybe a
concat maybeMaybeA =
  case maybeMaybeA of
    Nothing -> Nothing
    Just Nothing -> Nothing
    Just (Just v) -> Just v

concatMap : (a -> Maybe b) -> Maybe a -> Maybe b
concatMap f maybeA =
  maybeA
    |> Maybe.map f
    |> concat

isDefined : Maybe a -> Bool
isDefined maybeA =
  case maybeA of
    Just _ -> True
    Nothing -> False
