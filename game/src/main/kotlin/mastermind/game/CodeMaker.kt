package mastermind.game

fun makeCode(
    length: Int = 4,
    pegs: List<Code.Peg> = listOfPegs("Red", "Green", "Blue", "Yellow", "Purple")
) = Code((1..length).map { pegs.random() })
