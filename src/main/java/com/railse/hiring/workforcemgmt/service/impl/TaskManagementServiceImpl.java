package com.railse.hiring.workforcemgmt.service.impl;

import com.railse.hiring.workforcemgmt.common.exception.ResourceNotFoundException;
import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.mapper.ITaskManagementMapper;
import com.railse.hiring.workforcemgmt.model.ActivityLog;
import com.railse.hiring.workforcemgmt.model.Comment;
import com.railse.hiring.workforcemgmt.model.TaskManagement;
import com.railse.hiring.workforcemgmt.model.enums.Priority;
import com.railse.hiring.workforcemgmt.model.enums.Task;
import com.railse.hiring.workforcemgmt.model.enums.TaskStatus;
import com.railse.hiring.workforcemgmt.repository.TaskRepository;
import com.railse.hiring.workforcemgmt.service.TaskManagementService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TaskManagementServiceImpl implements TaskManagementService {

    private final TaskRepository taskRepository;
    private final ITaskManagementMapper taskMapper;

    public TaskManagementServiceImpl(TaskRepository taskRepository, ITaskManagementMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    public TaskManagementDto findTaskById(Long id) {
        TaskManagement task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return taskMapper.modelToDto(task);
    }

    //Feature 1
    @Override
    public List<TaskManagementDto> fetchTasksByDateEnhanced(TaskFetchByDateRequest request) {
        List<TaskManagement> tasks;

        //If assignee_ids are null or empty, fetch all
        if (request.getAssigneeIds() == null || request.getAssigneeIds().isEmpty()) {
            tasks = taskRepository.findAll();
        } else {
            tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());
        }

        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task ->
                        task.getStatus() != TaskStatus.CANCELLED && (
                                // Tasks starting within the given date range
                                (task.getTaskDeadlineTime() >= request.getStartDate() &&
                                        task.getTaskDeadlineTime() <= request.getEndDate()) ||

                                        // Tasks started before the range but still not completed
                                        (task.getTaskDeadlineTime() < request.getStartDate() &&
                                                task.getStatus() != TaskStatus.COMPLETED)
                        )
                )
                .collect(Collectors.toList());

        return taskMapper.modelListToDtoList(filteredTasks);
    }

    //2nd Feature
    @Override
    public String updateTaskPriority(UpdateTaskPriorityRequest request) {
        TaskManagement task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + request.getTaskId()));
        task.setPriority(request.getPriority());
        taskRepository.save(task);

        // For activity log purpose
        taskRepository.addActivity(new ActivityLog(
                task.getId(),
                "Priority changed to " + request.getPriority(),
                System.currentTimeMillis()
        ));

        return "Priority updated successfully for task ID " + request.getTaskId();
    }

    //2nd feature
    @Override
    public List<TaskManagementDto> getTasksByPriority(Priority priority) {
        List<TaskManagement> tasks = taskRepository.findByPriority(priority);
        return taskMapper.modelListToDtoList(tasks);
    }

    @Override
    public String addComment(AddCommentRequest request) {
        TaskManagement task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        Comment comment = new Comment(request.getTaskId(), request.getCommentText(), System.currentTimeMillis());
        taskRepository.addComment(comment);

        taskRepository.addActivity(new ActivityLog(request.getTaskId(),
                "User added comment: \"" + request.getCommentText() + "\"",
                System.currentTimeMillis()));

        return "Comment added";
    }

    @Override
    public TaskDetailsDto getTaskDetails(Long taskId) {
        TaskManagement task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        TaskManagementDto dto = taskMapper.modelToDto(task);
        List<CommentDto> comments = taskRepository.getCommentsForTask(taskId)
                .stream()
                .map(c -> new CommentDto(c.getCommentText(), c.getTimestamp()))
                .toList();

        List<ActivityLogDto> activities = taskRepository.getActivitiesForTask(taskId)
                .stream()
                .map(activityLog -> new ActivityLogDto(activityLog.getMessage(), activityLog.getTimestamp()))
                .toList();

        return new TaskDetailsDto(dto, comments, activities);
    }

    @Override
    public List<TaskManagementDto> createTasks(TaskCreateRequest createRequest) {
        List<TaskManagement> createdTasks = new ArrayList<>();
        for (TaskCreateRequest.RequestItem item : createRequest.getRequests()) {
            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(item.getReferenceId());
            newTask.setReferenceType(item.getReferenceType());
            newTask.setTask(item.getTask());
            newTask.setAssigneeId(item.getAssigneeId());
            newTask.setPriority(item.getPriority());
            newTask.setTaskDeadlineTime(item.getTaskDeadlineTime());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("New task created.");
            createdTasks.add(taskRepository.save(newTask));
        }
        return taskMapper.modelListToDtoList(createdTasks);
    }

    @Override
    public List<TaskManagementDto> updateTasks(UpdateTaskRequest updateRequest) {
        List<TaskManagement> updatedTasks = new ArrayList<>();
        for (UpdateTaskRequest.RequestItem item : updateRequest.getRequests()) {
            TaskManagement task = taskRepository.findById(item.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + item.getTaskId()));

            if (item.getTaskStatus() != null) {
                task.setStatus(item.getTaskStatus());
            }
            if (item.getDescription() != null) {
                task.setDescription(item.getDescription());
            }
            updatedTasks.add(taskRepository.save(task));
        }
        return taskMapper.modelListToDtoList(updatedTasks);
    }

    @Override
    public String assignByReference(AssignByReferenceRequest request) {
        List<Task> applicableTasks = Task.getTasksByReferenceType(request.getReferenceType());
        List<TaskManagement> existingTasks = taskRepository.findByReferenceIdAndReferenceType(request.getReferenceId(), request.getReferenceType());

        for (Task taskType : applicableTasks) {
            List<TaskManagement> tasksOfType = existingTasks.stream()
                    .filter(t -> t.getTask() == taskType && t.getStatus() != TaskStatus.COMPLETED)
                    .collect(Collectors.toList());

            // BUG #1 is here. It should assign one and cancel the rest.
            // Instead, it reassigns ALL of them.


            /*
            if (!tasksOfType.isEmpty()) {
                for (TaskManagement taskToUpdate : tasksOfType) {
                    taskToUpdate.setAssigneeId(request.getAssigneeId());
                    taskRepository.save(taskToUpdate);
                }
            } else {
                // Create a new task if none exist
                TaskManagement newTask = new TaskManagement();
                newTask.setReferenceId(request.getReferenceId());
                newTask.setReferenceType(request.getReferenceType());
                newTask.setTask(taskType);
                newTask.setAssigneeId(request.getAssigneeId());
                newTask.setStatus(TaskStatus.ASSIGNED);
                taskRepository.save(newTask);
            }

             */



            //BUG Fixed
            if (!tasksOfType.isEmpty()) {
                for (TaskManagement taskToUpdate : tasksOfType) {
                    taskToUpdate.setStatus(TaskStatus.CANCELLED);
                    taskRepository.save(taskToUpdate);
                }
            }
            // Create a new task if none exist
            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(request.getReferenceId());
            newTask.setReferenceType(request.getReferenceType());
            newTask.setTask(taskType);
            newTask.setAssigneeId(request.getAssigneeId());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setTaskDeadlineTime((System.currentTimeMillis()+86400000));
            taskRepository.save(newTask);



        }

        return "Tasks assigned successfully for reference " + request.getReferenceId();
    }

    @Override
    public List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request) {
        List<TaskManagement> tasks;

        //If assignee_ids are null or empty, fetch all
        if (request.getAssigneeIds() == null || request.getAssigneeIds().isEmpty()) {
            tasks = taskRepository.findAll();
        } else {
            tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());
        }

        // BUG #2 is here. It should filter out CANCELLED tasks but doesn't.

        /*
        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task -> {
                    // This logic is incomplete for the assignment.
                    // It should check against startDate and endDate.
                    // For now, it just returns all tasks for the assignees.
                    return true;
                })
                .collect(Collectors.toList());

         */




        //Bug fixed
        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task ->
                            task.getStatus() != TaskStatus.CANCELLED &&
                            task.getTaskDeadlineTime() >= request.getStartDate() &&
                            task.getTaskDeadlineTime() <= request.getEndDate()
                )
                .collect(Collectors.toList());



        return taskMapper.modelListToDtoList(filteredTasks);
    }
}

