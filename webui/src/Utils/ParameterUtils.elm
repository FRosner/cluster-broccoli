module Utils.ParameterUtils exposing (getOtherParameters)

import List exposing (filter, sortBy)
import String exposing (toLower)


{-| Get other parameters, in alphabetical order.

"Other" parameters are all parameters except "id" which receives special treatment of being shown
first in all cases.

Note: We sort parameters by their ID not by their user-visible name, so the sort order in the UI
might seem different.

-}
getOtherParameters : List String -> List String
getOtherParameters params =
    filter ((/=) "id") params |> sortBy toLower
