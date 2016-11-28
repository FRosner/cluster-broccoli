module Messages exposing (..)

import Players.Messages
import About.Messages
import Templates.Messages

type Msg
  = PlayersMsg Players.Messages.Msg
  | AboutMsg About.Messages.Msg
  | TemplatesMsg Templates.Messages.Msg
