package MCcrew.Coinportal.board;

import MCcrew.Coinportal.domain.Attachment;
import MCcrew.Coinportal.domain.Dto.PostDto;
import MCcrew.Coinportal.admin.AdminRepository;
import MCcrew.Coinportal.domain.Notice;
import MCcrew.Coinportal.domain.Post;
import MCcrew.Coinportal.photo.AttachmentRepository;
import MCcrew.Coinportal.photo.AttachmentService;
import MCcrew.Coinportal.user.UserRepository;
import MCcrew.Coinportal.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardRepository2 boardRepository2;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;
    private final AdminRepository adminRepository;

    private Logger logger = LoggerFactory.getLogger(this.getClass());


    @Value("${file.dir}")
    private String fileDirPath;

    public BoardService(BoardRepository boardRepository, BoardRepository2 boardRepository2, UserService userService, UserRepository userRepository, AttachmentService attachmentService, AttachmentRepository attachmentRepository, AdminRepository adminRepository) {
        this.boardRepository = boardRepository;
        this.boardRepository2 = boardRepository2;
        this.userService = userService;
        this.userRepository = userRepository;
        this.attachmentService = attachmentService;
        this.attachmentRepository = attachmentRepository;
        this.adminRepository = adminRepository;
    }

    /**
       ???????????? ????????? ??????
    */
    @Transactional
    public List<Post> searchPostsByKeyword(String keyword) {
        List<Post> postList = boardRepository2.findByContentContaining(keyword); // spring Data ??????
        if(postList.isEmpty())
            return new ArrayList<>();
        return postList;
    }

    /**
        ??????????????? ????????? ??????
     */
    @Transactional
    public List<Post> searchPostsByNickname(String nickname) {
        List<Post> postList = boardRepository2.findByUserNicknameContaining(nickname); // spring Data ??????
        if(postList.isEmpty()) return new ArrayList<>();
        return postList;
    }
    /**
       ??????????????? ????????? ??????
    */
    @Transactional
    public List<Post> searchPostsByPopularity() {
        List<Post> postList = boardRepository2.findAll(Sort.by(Sort.Direction.DESC, "upCnt"));
        if(postList.isEmpty())
            return new ArrayList<>();
        return postList;
    }

    /**
       ????????? ?????? ??????
     */
    public Post getSinglePost(Long id){
        return boardRepository.findById(id);
    }

    // ?????? ????????? ??????
    public List<Post> getAllPost(){
        try {
            return boardRepository.findAll();
        }catch(NoResultException e){
            return new ArrayList<>();
        }
    }

    /**
        ????????? ?????? ??? ??????
     */
    @Transactional
    public Post createPostByUser(PostDto userPostDto) {
        Date date = new Date();
        Post newPost = new Post();
        newPost.setUserId(userPostDto.getUserId());
        newPost.setUserNickname(userPostDto.getNickname());
        newPost.setBoardName(userPostDto.getBoardName());
        newPost.setGuestName(userPostDto.getGuestName());
        newPost.setGuestPwd(userPostDto.getGuestPwd());
        newPost.setContent(userPostDto.getContent());
        newPost.setUpCnt(0);
        newPost.setDownCnt(0);
        newPost.setViewCnt(0);
        newPost.setReportCnt(0);
        newPost.setCreatedAt(date);
        newPost.setUpdatedAt(date);
        newPost.setStatus('A');
        return boardRepository.save(newPost);
    }

    /**
        ????????? ????????? ??????
     */
    public Post updatePost(PostDto postDto, Long userId){
        Long postId = postDto.getPostId();
        Post findPost = boardRepository.findById(postId);
        if(findPost.getUserId() != userId){
            return new Post();
        }
        else{
            Date date = new Date();
            findPost.setContent(postDto.getContent());
            findPost.setBoardName(postDto.getBoardName());
            findPost.setUpdatedAt(date);
            return boardRepository.save(findPost);
        }
    }

    /**
        ????????? ????????? ?????????
     */
    public int likePost(Long postId){
        Post findPost = boardRepository.findById(postId);
        findPost.setUpCnt(findPost.getUpCnt() + 1);
        boardRepository.save(findPost);
        return findPost.getUpCnt();
    }
    /**
        ????????? ????????? ?????????
     */
    public int dislikePost(Long postId){
        Post findPost = boardRepository.findById(postId);
        findPost.setDownCnt(findPost.getDownCnt() + 1);
        boardRepository.save(findPost);
        return findPost.getDownCnt();
    }

    /**
        ????????? ????????? ??????
     */
    public int reportPost(Long postId) throws Exception{
        Post findPost = boardRepository.findById(postId);
        findPost.setReportCnt(findPost.getReportCnt() + 1);
        return findPost.getReportCnt();
    }

    /**
        ????????? ????????? ????????? ??????
     */
    public void viewPost(Long postId) {
        Post findPost = boardRepository.findById(postId);
        findPost.setViewCnt(findPost.getViewCnt() + 1);
        boardRepository.save(findPost);
    }

    /**
        ?????? ??????
     */
    public boolean deleteFile(String fileName){
        try {
            File file = new File(fileDirPath + fileName);
            if (file.delete()) { // ?????? ????????? ???????????? true, ???????????? false
                logger.info("delete successfully");
                return true;
            } else {
                return false;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /**
       ????????? ????????? ?????? ??????
    */
    public boolean deletePost(Long postId) {
        Post findPost = boardRepository.findById(postId);
        List<Attachment> attachmentList = attachmentRepository.findByPost_Id(postId);
        boolean deleted = false;
        for(int i = 0; i < attachmentList.size(); i++){
            logger.info("deleting " + attachmentList.get(i).getStoreFilename());
            deleted = deleteFile(attachmentList.get(i).getStoreFilename());
            attachmentRepository.deleteById(attachmentList.get(i).getId());
        }
        if(!deleted){
            return false;
        }
        int deletedPost = boardRepository.delete(postId);
        if(deletedPost > 0){ // delete ??? ????????? ???????????????
            logger.info("???????????? ???????????????.");
            return true;
        }else{
            logger.info("????????? ????????? ??????????????????.");
            return false;
        }
    }

    /**
        ?????? ????????? ?????? - deprecated
     */
    public boolean status2Delete(Long postId, Long userId) {
        Post findPost = boardRepository.findById(postId);
        if(findPost.getUserId() != userId){
            return false;
        }else{
            findPost.setStatus('D');
            boardRepository.save(findPost);
            return true;
        }
    }

    private static final int BLOCK_PAGE_NUM_COUNT = 5; // ????????? ???????????? ????????? ?????? ???
    private static final int PAGE_POST_COUNT = 4;       // ??? ???????????? ???????????? ????????? ???

    /**
        ???????????? ????????? ?????????
     */
    @Transactional
    public List<Post> getPostlist(String boardName, int pageNum) {
        Page<Post> page = boardRepository2.findByBoardName(boardName, PageRequest.of(pageNum - 1, PAGE_POST_COUNT, Sort.by(Sort.Direction.ASC, "createdAt")));
        List<Post> postList = page.getContent();
        return postList;
    }

    /**
        ?????? ????????? ??????
     */
    @Transactional
    public Long getBoardCount() {
        return boardRepository2.count();
    }

    /**
        ???????????? ????????? ??????
     */
    @Transactional
    public int getBoardCountByBoardName(String boardName){
        return boardRepository.findByBoardName(boardName).size();
    }

    /**
        ????????? ?????? ?????????
     */
    public int[] getPageList(String boardName, int curPageNum) {
        int[] pageList = new int[BLOCK_PAGE_NUM_COUNT]; // 5

        // ??? ????????? ??????
        Double postsTotalCount = Double.valueOf(this.getBoardCountByBoardName(boardName));
        logger.info("getPageList: total post count: " + postsTotalCount);

        // ??? ????????? ???????????? ????????? ????????? ????????? ?????? ?????? (???????????? ??????)
        Integer totalLastPageNum = (int)(Math.ceil((postsTotalCount / PAGE_POST_COUNT)));  // postsTotalCount/4
        logger.info("last page num: " + totalLastPageNum);

        // ?????? ???????????? ???????????? ????????? ????????? ????????? ?????? ??????
        Integer blockLastPageNum = (totalLastPageNum > curPageNum + BLOCK_PAGE_NUM_COUNT) // 13 > 2 + 5
                ? curPageNum + BLOCK_PAGE_NUM_COUNT // curPageNum + 5 -> 7
                : totalLastPageNum;                 // totalLastPageNum -> 13
        logger.info("last num of block: " + blockLastPageNum);
        // ????????? ?????? ?????? ??????
        curPageNum = (curPageNum <= 3) ? 1 : curPageNum - 2;
        logger.info("page start num: ", curPageNum);

        // ????????? ?????? ??????
        try{
            for (int val = curPageNum, idx = 0; val <= blockLastPageNum; val++, idx++) { // 1 ~ 7
                if(idx == 5)
                    break;
                pageList[idx] = val;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        logger.info("page allocation num:"+ pageList.toString());
        return pageList;
    }

    /**
        ?????? ???????????????
     */
    public Post post(PostDto postDto, Long userIdx) throws IOException {
        Date date = new Date();

        Post post = new Post();
        post.setUserId(userIdx);
        post.setComments(new ArrayList<>());
        post.setUserNickname(postDto.getNickname());
        post.setBoardName(postDto.getBoardName());
        post.setGuestName(postDto.getGuestName());
        post.setGuestPwd(postDto.getGuestPwd());
        post.setContent(postDto.getContent());
        post.setUpCnt(0);
        post.setDownCnt(0);
        post.setViewCnt(0);
        post.setReportCnt(0);
        post.setCreatedAt(date);
        post.setUpdatedAt(date);
        post.setStatus('A');

        Post savedPost = boardRepository.save(post);
        List<Attachment> attachments = attachmentService.saveAttachments(postDto.getAttachedFiles(), savedPost.getId());
        savedPost.setAttachedFiles(attachments);
        return boardRepository.save(savedPost);
    }

    /**
        ?????? ????????? ????????? ??????
     */
    public List<Post> getMyPost(Long userId) {
        return boardRepository.findByUserId(userId);
    }

    /**
        ?????? ????????? ????????????
     */
    public List<Notice> getNotice() {
        try{
            return adminRepository.findAll();
        }catch(NoResultException e){
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
