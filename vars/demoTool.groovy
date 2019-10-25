import groovy.json.JsonOutput

/**
 * downloads package from artifactory
 * @param config : Map object
 * @param toolsArtifactPath : pattern to download package from artifactory.
 * @return downloadInfo
 * Every global function that calls tmoTools must include these below three parameters.
 * downloadTo: [
        description: 'Specify the target path in workspace to download artifacts.',
        default: 'edp-tools/',]
   artifactoryServerURL: [
       description: 'URL for the artifactory server.',
       default: '',]
   artifactoryCred: [
       description: 'Credentials to use for downloading tools from artifactory.  A default is provided.',
       default: null, ]
 */
def downloadArtifact(config, toolsArtifactPath){
    
    def server = Artifactory.newServer url: config.artifactoryServerURL, credentialsId: artifactoryCred
    def downloadSpec = JsonOutput.toJson([
            files: [
                    [
                            pattern: toolsArtifactPath,
                            target: "${config.downloadTo}",
                    ],
            ],
    ])

    def downloadInfo = server.download spec: downloadSpec
    downloadInfo
}

/**
 * downloads package from artifactory and untar's content in to the jenkins workspace and return folder path
 * @param config : Map object
 * @param toolName : toolName which need to be download.
 * @return toolFolderPath return tool folder path
 */
def downloadInstallTool(config, toolName){
    if(!env[toolName]){
        def toolsArtifactPath = getToolArtifactoryPath(toolName)
        downloadArtifact(config, toolsArtifactPath)
        def relPathWithfileName = "${toolsArtifactPath}"
        def toolFolderPath = untarDownloadedFile(relPathWithfileName)
        echo "toolFolderPath in workspace: ${toolFolderPath}"
        env[toolName] = toolFolderPath
    }
    return env[toolName]
}

/**
 * untar the package to the jenkins workspace and return's absolute path
 * @param relPathWithfileName : relative path with filename.
 * @return toolFolderPath return tool folder path
 */
def untarDownloadedFile(relPathWithfileName){
   def absPathWithFileName = env.WORKSPACE + '/' + relPathWithfileName
   def absPath = '/usr/bin'
   def toolFolderName = sh (returnStdout: true, script:  """
                            FOLDER_NAME=`tar tzf "${absPathWithFileName}" | sed -e 's@/.*@@' | uniq`
                            tar -zxf ${absPathWithFileName} -C ${absPath}
                            rm $absPathWithFileName
                            echo "\$FOLDER_NAME"
                        """).tokenize().last()
    def absPathToTool=absPath+'/'+toolFolderName
    echo "absPathToTool : $absPathToTool"
    return absPathToTool
}

/**
 * creates pipeline-config directory and clones the pipeline-global-config.git repo into it.
 * reads Json file and returns path to tool
 * @param toolName : toolName from allowed-tools.json file.
 * @return path for toolname specified.
 */
def getToolArtifactoryPath(toolName){
    library 'pipeline-config'
    props = readJSON file: 'allowed-tools.json'
    if(!props[toolName]){
        currentBuild.result = 'ABORTED'
        error "Tool path not found.  Make sure tool has been configured in allowed-tools.json."
    }
    props[toolName.toString()]
}