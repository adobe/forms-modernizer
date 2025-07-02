/**~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2024 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

'use strict';

const e = require('child_process');
const fs = require('fs');
const path = require('path');
const https = require('https');

module.exports = class CI {
    constructor() {
        this.maxConfigDepth = 5; // Maximum depth for configuration collection
    }

    /**
     * Print build context to stdout.
     */
    context() {
        try {
            this.sh('java -version');
            this.sh('mvn -v');
            console.log("Node version: %s", process.version);
            this.sh('printf "NPM version: $(npm --version)"', false, false);
        } catch (error) {
            console.error('Failed to get build context:', error);
            throw error;
        }
    }

    /**
     * Switch working directory for the scope of the given function.
     */
    dir(dir, func) {
        let currentDir = process.cwd();
        process.chdir(dir);
        console.log('// Changed directory to: ' + process.cwd());
        try {
            func();
        } finally {
            process.chdir(currentDir);
            console.log('// Changed directory back to: ' + currentDir);
        }
    }

    /**
     * Checkout git repository with the given branch into the given folder.
     */
    checkout(repo, branch = 'master', folder = '') {
        this.sh('git clone -b ' + branch + ' ' + repo + ' ' + folder);
    }

    /**
     * Run shell command and attach to process stdio.
     */
    sh(command, returnStdout = false, print = true, shellStr = '') {
        if (print) {
            console.log(command);
        }
        try {
            if (returnStdout) {
                return e.execSync(command).toString().trim();
            }
            if (shellStr) {
                return e.execSync(command, {stdio: 'inherit', shell: shellStr});
            } else {
                return e.execSync(command, {stdio: 'inherit'});
            }
        } catch (error) {
            console.error(`Command failed: ${command}`);
            console.error(`Error: ${error.message}`);
            throw error;
        }
    }

    /**
     * Return value of given environment variable.
     */
    env(key) {
        const value = process.env[key];
        if (!value) {
            throw new Error(`Required environment variable ${key} is not set`);
        }
        return value;
    }

    /**
     * Print stage name.
     */
    stage(name) {
        console.log("\n------------------------------\n" +
            "--\n" +
            "-- %s\n" +
            "--\n" +
            "------------------------------\n", name);
    }

    /**
     * Configure a git impersonation for the scope of the given function.
     */
    gitImpersonate(user, mail, func) {
        try {
            this.sh('git config --local user.name ' + user + ' && git config --local user.email ' + mail, false, false);
            func();
        } finally {
            this.sh('git config --local --unset user.name && git config --local --unset user.email', false, false);
        }
    }

    /**
     * Configure git credentials for the scope of the given function.
     */
    gitCredentials(repo, func) {
        try {
            this.sh('git config credential.helper \'store --file .git-credentials\'');
            fs.writeFileSync('.git-credentials', repo);
            console.log('// Created file .git-credentials.');
            func();
        } finally {
            this.sh('git config --unset credential.helper');
            fs.unlinkSync('.git-credentials');
            console.log('// Deleted file .git-credentials.');
        }
    }

    /**
     * Writes given content to a file.
     */
    writeFile(fileName, content) {
        console.log(`// Write to file ${fileName}`);
        fs.writeFileSync(fileName, content, { 'encoding': 'utf8' });
    }

    collectConfiguration() {
        let configuration = {
            modules: {}
        };

        const processDirectory = (folder, depth = 0) => {
            if (depth > this.maxConfigDepth) {
                console.log(`Skipping directory ${folder} - max depth reached`);
                return;
            }

            let files = fs.readdirSync(folder, { withFileTypes: true });

            for (let file of files) {
                if (file.isDirectory()) {
                    processDirectory(path.resolve(folder, file.name), depth + 1);
                    continue;
                }

                if (file.name !== 'pom.xml') {
                    continue;
                }

                let pomPath = path.resolve(folder, file.name);
                try {
                    let metaData = this.sh('printf \'${project.groupId}|${project.artifactId}|${project.name}|${project.version}|${project.packaging}\' | mvn -f ' + pomPath + ' help:evaluate --non-recursive | grep -Ev "(Download|\\[)"', true, false).split('|');
                    configuration.modules[metaData[1]] = {
                        groupId: metaData[0],
                        artifactId: metaData[1],
                        name: metaData[2],
                        version: metaData[3],
                        packaging: metaData[4],
                        path: folder
                    };
                    process.stdout.write('.');
                } catch (error) {
                    console.error(`Failed to process pom.xml at ${pomPath}:`, error);
                }
            }
        };

        process.stdout.write("Collecting project configuration");
        processDirectory(process.cwd());
        process.stdout.write(require('os').EOL);
        
        try {
            fs.writeFileSync('configuration.json', JSON.stringify(configuration, null, 4));
        } catch (error) {
            console.error('Failed to write configuration.json:', error);
            throw error;
        }

        return configuration;
    }

    restoreConfiguration() {
        try {
            let configuration = fs.readFileSync('configuration.json');
            return JSON.parse(configuration);
        } catch (error) {
            console.error('Failed to restore configuration:', error);
            throw error;
        }
    }

    addQpFileDependency(module, cloud = false) {
        let output = '--install-file ';

        let filename = `${module.artifactId}-${module.version}`;
        if (cloud) {
            filename += '-cloud';
        }
        if (module.packaging == 'content-package') {
            filename += '.zip';
        } else if (module.packaging == 'bundle') {
            filename += '.jar';
        }

        output += path.resolve(module.path, 'target', filename);

        return output;
    }

    async postCommentToGitHubFromCI(commentText) {
        const {CIRCLE_PROJECT_USERNAME, CIRCLE_PROJECT_REPONAME, CIRCLE_PULL_REQUEST, GITHUB_TOKEN} = process.env;
        
        if (!CIRCLE_PULL_REQUEST) {
            console.log('Not a pull request, skipping comment');
            return;
        }

        if (!GITHUB_TOKEN) {
            throw new Error('GITHUB_TOKEN environment variable is required for posting comments');
        }

        const prNumber = CIRCLE_PULL_REQUEST.split('/').pop();
        const apiUrl = new URL(`https://api.github.com/repos/${CIRCLE_PROJECT_USERNAME}/${CIRCLE_PROJECT_REPONAME}/issues/${prNumber}/comments`);
        const postData = JSON.stringify({body: commentText});

        return new Promise((resolve, reject) => {
            const options = {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${GITHUB_TOKEN}`,
                    'User-Agent': 'CircleCI',
                    'Content-Type': 'application/json'
                }
            };

            const req = https.request(apiUrl, options, (res) => {
                let data = '';
                res.on('data', (chunk) => {
                    data += chunk;
                });
                res.on('end', () => {
                    if (res.statusCode >= 200 && res.statusCode < 300) {
                        console.log(`Successfully posted comment to PR #${prNumber}`);
                        resolve(data);
                    } else {
                        reject(new Error(`Failed to post comment: ${res.statusCode} ${data}`));
                    }
                });
            });

            req.on('error', (error) => {
                reject(error);
            });

            req.write(postData);
            req.end();
        });
    }
};