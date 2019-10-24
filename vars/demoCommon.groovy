/**
 * Parse arguments contained in a function body, according to a
 * description of the desired arguments.  Returns a map of those
 * arguments.
 *
 * @param argDesc A description of the arguments.  This must be a map
 *                containing the following keys: "name" (required; the
 *                name of the class the arguments are for);
 *                "description" (optional; a description of the
 *                purpose of the class); "url" (optional; a URL
 *                containing more documentation or pointing to an
 *                example); and "args" (required).  The "args" key
 *                contains a map from argument names to a map with the
 *                following keys: "description" (required; a
 *                description of the argument); "error" (optional;
 *                text of the error message; if not provided, a
 *                message utilizing the description will be used);
 *                "default" (optional; the default value; if not
 *                provided, the argument will be required); and
 *                "validate" (optional; a closure of one argument that
 *                will be passed the value and must return it suitably
 *                transformed).
 * @param body The closure containing the arguments.
 *
 * @return A map containing arguments and their values, as transformed
 *         by the "validate" closure if one was provided.
 */
def parseArgs(argDesc, body) {
    //logging TransactionId
    logTransactionId()
    // First, evaluate the body and grab the raw values
    def raw = [:]
    if (body) {
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = raw
        body()
     }

    // Now, process the arguments in turn
    def args = [:]
    argDesc.args.each { name, desc ->
        // Check if we have a value
        if (raw.containsKey(name)) {
            def value = raw[name]

            // Pass it through the validator if there is one
            if (desc.containsKey('validate')) {
                value = desc.validate(value)
            }

            args[name] = value
        } else if (desc.containsKey('default')) {
            // Assign the default
            args[name] = desc['default']
        } else {
            // OK, the value is required, but not provided.  Get an
            // error message
            def errMsg
            if (desc.containsKey('error')) {
                errMsg = desc.error
            } else {
                errMsg = "${argDesc.name} missing required parameter ${name}.  ${desc.description}"
            }

            // Mark the build aborted and output the error message
            currentBuild.result = 'ABORTED'
            error errMsg
        }
    }
    return args
}

/**
 * logs and returns transactionId created using jenkins job start time and gitsha
 * @return transactionId
 */
def logTransactionId(){
    if (env.NODE_NAME && fileExists(file: '.git') && !(env.TRANSACTION_ID) ) {
        def gitSha = sh returnStdout: true, script: 'git rev-parse HEAD'
        def startTimeInMillis = currentBuild.startTimeInMillis
        Date date = new Date(startTimeInMillis)
        def formattedDateTime = date.format("yyyy-MM-dd'T'hh:mm:ss'Z'", TimeZone.getTimeZone('UTC'));
        gitSha = gitSha.substring(0, 8)
        env.TRANSACTION_ID = "${formattedDateTime}-${gitSha}"
        echo "Pipeline Transaction ID ${env.TRANSACTION_ID}"
        }
    env.TRANSACTION_ID
}
