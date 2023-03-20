package se.attini.cli;

import java.util.Collections;
import java.util.Iterator;

import software.amazon.awssdk.profiles.ProfileFile;

public class ProfileCompletionCandidates implements Iterable<String> {

    @Override
    public Iterator<String> iterator() {
        try{
            return ProfileFile.defaultProfileFile().profiles().keySet().iterator();
        }catch (Exception e){
            return Collections.emptyIterator();
        }
    }
}
