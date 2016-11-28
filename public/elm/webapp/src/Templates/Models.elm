module Templates.Models exposing (..)

type alias Template =
  { id : String
  , description : String
  , version : String
  }

type alias Templates = List Template
