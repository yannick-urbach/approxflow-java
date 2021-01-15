import urbachyannick.approxflow.*;

@PublicOutput
var outVar: Int = 0;

@PrivateInput
fun inFun(): Int {
    return 0;
}

fun main() {
    outVar = inFun();
}