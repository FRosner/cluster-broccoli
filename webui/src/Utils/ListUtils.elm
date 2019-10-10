module Utils.ListUtils exposing (remove)


remove i xs =
    List.take i xs ++ List.drop (i + 1) xs
