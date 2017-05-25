package onlinejudge.judge.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import onlinejudge.contest.client.ResourceClient;
import onlinejudge.contest.client.SubmitClient;
import onlinejudge.domain.Problem;
import onlinejudge.domain.Submit;
import onlinejudge.domain.TestCase;
import onlinejudge.dto.MyResponse;
import onlinejudge.file.dto.GroupResource;
import onlinejudge.file.dto.MyResource;

@Service
public class JudgeService {
	
	@Value("${path.submit}")
	public String submitRootPath;;
	@Value("${path.data}")
	public String dataRootPath ;
	
	@Autowired
	SubmitClient submitClient;
	
	@Autowired
	ResourceClient resourceClient;
	
	public Submit submit(Submit submit){
		return submitClient.submit(submit);
	}
	
	public Problem getProblemByProblemForContestId(String problemForContestId){
		Map<String, Object> param = new HashMap<>();
		param.put("problemForContestId", problemForContestId);
		return submitClient.getProblemByProblemForContestId(param);
	}
	
	public void prepareTestCase(List<TestCase> listTestCase) throws IOException{
		GroupResource groupResource  = new GroupResource();
		for (TestCase testCase : listTestCase) {
			if(!new File(dataRootPath + "/" + testCase.getInputFilePath()).exists())
				groupResource.add(new MyResource(MyResource.RESOURCE_TYPE_TESTCASE_INPUT,testCase.getInputFilePath()));
			if(!new File(dataRootPath + "/" + testCase.getOutputFilePath()).exists())
				groupResource.add(new MyResource(MyResource.RESOURCE_TYPE_TESTCASE_OUTPUT,testCase.getOutputFilePath()));	
		}
		if(!groupResource.getListResource().isEmpty()){
			groupResource = resourceClient.downfile(groupResource);
			for (MyResource resource : groupResource.getListResource()) {
				FileUtils.copyInputStreamToFile(resource.inputStream(),new File(dataRootPath + "/" + resource.getFileName()));
			}
		}
	}
	public void uploadSubmitFileToResourceServer(String fileName, byte[] data) throws Exception{
		GroupResource groupResource  = new GroupResource();
		MyResource submitFile = new MyResource();
		submitFile.setFileName(fileName);
		submitFile.setData(data);
		submitFile.setResourceType(MyResource.RESOURCE_TYPE_SUBMIT);
		groupResource.add(submitFile);
		
		MyResponse myResponse = resourceClient.upfile(groupResource);
		if(myResponse.getCode() != MyResponse.CODE_SUCCESS)
			throw new Exception("Can't connect to resource server!");
	}
}
