module Utils.ParameterUtils exposing (getOtherParametersSorted)

import List
import String
import Tuple


{-| Get other parameters, in order of the order-index and then alphabetical order.

"Other" parameters are all parameters except "id" which receives special treatment of being shown
first in all cases.

Note: We sort parameters by their ID not by their user-visible name, so the sort order in the UI
might seem different.

-}
getOtherParametersSorted : List ( Float, String ) -> List String
getOtherParametersSorted params =
    List.filter (\( orderIndex, param ) -> param /= "id") params
        |> List.sortBy (\( orderIndex, param ) -> ( orderIndex, String.toLower param ))
        |> List.map Tuple.second
