package onlinejudge.judge.controller;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import onlinejudge.domain.Problem;
import onlinejudge.domain.Submit;
import onlinejudge.domain.TestCase;
import onlinejudge.judge.process.Callback;
import onlinejudge.judge.process.MyCompiler;
import onlinejudge.judge.process.MyJudge;
import onlinejudge.judge.service.JudgeService;
@Controller
public class JudgeController {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Value("${path.submit}")
	public String submitRootPath;;
	@Value("${path.data}")
	public String dataRootPath ;
	
	@Autowired
	JudgeService judgeService;
	
	@RequestMapping(value = { "/submit" }, method = RequestMethod.POST)
	public @ResponseBody Submit submit(MultipartHttpServletRequest request, Model model) throws Exception {
		Submit submit = new Submit();
		
		MultipartFile file = request.getFile("file");
		logger.debug("Recieve submit with file size is: " + (file == null ? 0 : file.getSize()));
		
		long idSubmit = System.currentTimeMillis();
		String filePath = submitRootPath + "/" + idSubmit + "/" + file.getOriginalFilename();
		
		try {
			logger.debug("Store file to local is starting...");
			FileUtils.writeByteArrayToFile(new File(filePath), file.getBytes()); //store local
			logger.debug("Store file to local is completed");
			logger.debug("Store file to resource is starting...");
			judgeService.uploadSubmitFileToResourceServer(idSubmit + "/" + file.getOriginalFilename(), file.getBytes()); // store to resource server
			logger.debug("Store file to resource server is completed");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw e;
		}
		
		//add Submit to ProblemForTeam
		submit.setDateSubmit(new Date());
		submit.setFilePath(idSubmit + "/" + file.getOriginalFilename());
		submit.setIdContest(request.getParameter("idContest"));
		submit.setIdTeam(request.getParameter("idTeam"));
		submit.setIdProblemForTeam(request.getParameter("idProblemForTeam"));
		submit.setStatus("Judging");
		
		submit = judgeService.submit(submit);
		
		String strOverrideTestCase = request.getParameter("isOverrideTestCase");
		boolean isOverrideTestCase = StringUtils.isEmpty(strOverrideTestCase) ? false: Boolean.parseBoolean(strOverrideTestCase);
		
		Problem problem = judgeService.getProblemByProblemForContestId(submit.getIdProblemForTeam());
		judgeService.prepareTestCase(problem.getListTestCase());

		final boolean[] isCompileComplete = new boolean[] { false };
		logger.debug("Compiler is starting...");
		MyCompiler myCompiler = new MyCompiler(submitRootPath + "/" + idSubmit, file.getOriginalFilename(),
				new Callback() {
					@Override
					public void complete() {
						isCompileComplete[0] = true;
						logger.debug("Compiler is complete.");
					}
				});
		myCompiler.run();

		while (!isCompileComplete[0]) {
			Thread.sleep(100);
		}

		boolean isCompileSuccess = myCompiler.isCompileSuccess();
		logger.debug("Compiler'status is: " + isCompileSuccess);
		
		TestCase testCase = problem.getListTestCase().get(0);
		
		if (isCompileSuccess) {

			final boolean[] isJudgeComplete = new boolean[] { false };
			logger.debug("Judge host is starting...");
			MyJudge myJudge = new MyJudge(submitRootPath + "/" + idSubmit,dataRootPath,
					getFileNameWithoutExtension(file.getOriginalFilename()), 
					testCase.getInputFilePath(), testCase.getOutputFilePath(), problem.getTimeLimit(),
					new Callback() {
						public void complete() {
							isJudgeComplete[0] = true;
							logger.debug("Judge host is complete.");
						}
					});
			myJudge.run();
			while (!isJudgeComplete[0]) {
				Thread.sleep(100);
			}
			if (myJudge.isCorrect()) {
				submit.setStatus("Accepted");
				submit.setResolve(true);
			} else if (myJudge.isIncorrect()) {
				submit.setStatus("WrongAnswer");
				submit.setResolve(false);
			} else if (myJudge.isError()) {
				submit.setStatus("Error");
				submit.setResolve(false);
				submit.setErrorMessage(myJudge.getErrorMessage());;
			}else if(myJudge.isTimeOut()){
				submit.setStatus("TimeOut");
				submit.setResolve(false);
			}
			submit.setTimeToRun(myJudge.getTimeExecuted());
			logger.debug("Judge host'information is: " + myJudge.getInformation());
		} else {
			submit.setStatus("Error");
			submit.setResolve(false);
			submit.setErrorMessage("Compile fail");
		}
		judgeService.submit(submit);
		return submit;
	}
	private String getFileNameWithoutExtension(String fileName) {
		return fileName.substring(0, fileName.lastIndexOf('.'));
	}
}
