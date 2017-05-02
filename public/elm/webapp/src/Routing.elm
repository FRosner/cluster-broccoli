module Routing exposing (..)

import Navigation exposing (Location)
import Model exposing (Route(..))
import UrlParser exposing (..)

matchers : Parser (Route -> a) a
matchers =
  oneOf
    [ map MainRoute top
    ]

parseLocation : Location -> Route
parseLocation location =
  case (parseHash matchers location) of
    Just route ->
      route
    Nothing ->
      MainRoute -- main route is always fine :)
