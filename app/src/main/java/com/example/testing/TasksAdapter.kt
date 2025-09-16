package com.example.testing

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TasksAdapter(
    private val onCheckedChange: (Task, Boolean) -> Unit,
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : ListAdapter<Task, TasksAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: CheckBox = itemView.findViewById(R.id.taskName)
        private val description: TextView = itemView.findViewById(R.id.taskDescription)
        private val dueDate: TextView = itemView.findViewById(R.id.taskDueDate)
        private val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(task: Task) {
            name.text = task.name
            name.isChecked = task.isCompleted
            name.paintFlags = if (task.isCompleted) name.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG else name.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            description.text = task.description
            description.visibility = if (task.description.isNullOrEmpty()) View.GONE else View.VISIBLE

            if (task.dueAtMillis != null) {
                val df = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
                dueDate.text = df.format(Date(task.dueAtMillis))
                dueDate.visibility = View.VISIBLE
            } else {
                dueDate.visibility = View.GONE
            }

            name.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(task, isChecked)
            }
            editButton.setOnClickListener { onEditClick(task) }
            deleteButton.setOnClickListener { onDeleteClick(task) }
        }
    }
}

class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem == newItem
    }
}