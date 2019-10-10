module Models.Ui.InstanceParameterForm exposing (..)

import Dict exposing (Dict)
import Maybe exposing (Maybe)
import Models.Resources.Template exposing (ParameterValue, Template)



-- ParameterValues are kept in String here as they must be displayed in the input
-- If the ParameterValue was invalid (eg: String when expectation was IntParamVal)
-- we cannot make the user input go away. So we store the raw format in a String.


type alias InstanceParameterForm =
    { originalParameterValues : Dict String (Maybe String)
    , changedParameterValues : Dict String (Maybe String)
    , selectedTemplate : Maybe Template
    }


empty =
    InstanceParameterForm
        Dict.empty
        Dict.empty
        Nothing
