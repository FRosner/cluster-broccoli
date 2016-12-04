module Models.Ui.LoginForm exposing (..)

type alias LoginForm =
  { username : String
  , password : String
  , loginIncorrect : Bool
  }

emptyLoginForm = LoginForm "" "" False
