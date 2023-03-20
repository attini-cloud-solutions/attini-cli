package se.attini.pack;

public class SamPackageCommand {


    public final static String PACKAGE_COMMANDS =
            """
                    set -eo pipefail
                    export=SAM_CLI_TELEMETRY=0
                                        
                    for prj in $(jq '.samProjects[]' -c attini_data/attini-metadata.json); do
                      TEMP=$(echo $prj | jq .path -r);
                      SAM_PROJECT_PATH="${TEMP:1}"
                      SAM_BUILD_DIR=$(echo $prj | jq .buildDir -r);
                      echo "SAM package for project: ${SAM_PROJECT_PATH}"
                      unzip -qo ${SAM_PROJECT_PATH}/attiniSamProject.zip -d ${SAM_PROJECT_PATH}
                      S3_PREFIX="${ATTINI_ENVIRONMENT_NAME}/${ATTINI_DISTRIBUTION_NAME}/${ATTINI_DISTRIBUTION_ID}/.sam-source/${SAM_PROJECT_PATH}"
                      sam package -t ${SAM_PROJECT_PATH}/${SAM_BUILD_DIR}/template.yaml \\
                        --s3-bucket "${ATTINI_ARTIFACT_STORE}" \\
                        --s3-prefix "${S3_PREFIX}" \\
                        --output-template-file "template.yaml"
                      aws s3 cp template.yaml "s3://${ATTINI_ARTIFACT_STORE}/${S3_PREFIX}/template.yaml"
                    done
                                        
                    OBJECT_ID=$(cat $ATTINI_INPUT| jq .deploymentOriginData.objectIdentifier -r)
                    ITEMS=$(aws dynamodb query \\
                        --table-name AttiniDeployDataV1 \\
                        --projection-expression "deploymentTime" \\
                        --index-name objectIdentifier \\
                        --key-condition-expression "deploymentName = :deployName AND objectIdentifier = :objectId" \\
                        --expression-attribute-values "{\\":deployName\\": {\\"S\\": \\"${ATTINI_ENVIRONMENT_NAME}-${ATTINI_DISTRIBUTION_NAME}\\"}, \\":objectId\\": {\\"S\\": \\"$OBJECT_ID\\"}}" | jq .Items)
                                        
                    for itm in $(echo $ITEMS | jq '.[]' -c); do
                      SORT_KEY=$(echo $itm | jq .deploymentTime.N -r)
                      aws dynamodb update-item \\
                          --table-name AttiniDeployDataV1 \\
                          --key "{\\"deploymentName\\": {\\"S\\": \\"${ATTINI_ENVIRONMENT_NAME}-${ATTINI_DISTRIBUTION_NAME}\\"}, \\"deploymentTime\\": {\\"N\\": \\"$SORT_KEY\\"}}" \\
                          --update-expression 'SET #samPackaged = :samPackaged' \\
                          --expression-attribute-names "{\\"#samPackaged\\": \\"samPackaged\\"}" \\
                          --expression-attribute-values "{\\":samPackaged\\": {\\"BOOL\\": true}}"
                    done
                                        
                    """;
}



