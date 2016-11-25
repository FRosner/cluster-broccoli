module Messages exposing (..)

import Players.Messages
import About.Messages

type Msg
    = PlayersMsg Players.Messages.Msg
    | AboutMsg About.Messages.Msg
