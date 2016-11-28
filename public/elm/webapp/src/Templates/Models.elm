module Templates.Models exposing (..)

type alias Template =
  { id : String
  , description : String
  }

type alias Templates = List Template
