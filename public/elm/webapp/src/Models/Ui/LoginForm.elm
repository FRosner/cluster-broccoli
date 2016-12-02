module Models.Ui.LoginForm exposing (..)

type alias LoginForm =
  { username : String
  , password : String
  }

emptyLoginForm = LoginForm "" ""
